package org.onebusaway.nyc.gtfsrt.controller;

import org.apache.commons.lang3.StringUtils;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.BundleManagementServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.utility.DateLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;

@Controller
public class BundleManagementController {

  @Autowired
  private BundleManagementServiceImpl _bundleManager;

  // for integration testing only
  @RequestMapping(value = "/change-bundle.do", method = RequestMethod.GET)
  public ModelAndView index(@RequestParam String bundleId, 
      @RequestParam(required=false) String time) throws Exception {

    if(time != null && !StringUtils.isEmpty(time)) {
      _bundleManager.setTime(DateLibrary.getIso8601StringAsTime(time));
    } else {
      _bundleManager.setTime(new Date());
    }

    _bundleManager.changeBundle(bundleId);

    return new ModelAndView("bundle-change.jspx");
  }

  @RequestMapping("/bundles.do")
  public ModelAndView index() {
    return new ModelAndView("bundles.jspx", "bms", _bundleManager);
  }

  @RequestMapping("/bundles!discover.do")
  public ModelAndView rediscover() throws Exception {
    _bundleManager.discoverBundles();

    return new ModelAndView("redirect:/bundles.do");
  }

  @RequestMapping("/bundles!reassign.do")
  public ModelAndView reassign(@RequestParam(required=false) String time) throws Exception {
    if(time != null && !StringUtils.isEmpty(time)) {
      _bundleManager.setTime(DateLibrary.getIso8601StringAsTime(time));
    } else {
      _bundleManager.setTime(new Date());
    }

    _bundleManager.refreshApplicableBundles();
    _bundleManager.reevaluateBundleAssignment();
    
    return new ModelAndView("redirect:/bundles.do");
  }

  @RequestMapping("/bundles!change.do")
  public ModelAndView change(@RequestParam String bundleId, @RequestParam(required=false) String time, @RequestParam(required=false) boolean automaticallySetDate) throws Exception {
    if(time != null && !StringUtils.isEmpty(time)) {
      _bundleManager.setTime(DateLibrary.getIso8601StringAsTime(time));
    } else {
      _bundleManager.setTime(new Date());
    }

    // if automaticallySetDate == true, we set the date to what it needs to be to have the bundle
    // change succeed
    if(automaticallySetDate == true) {
      List<BundleItem> bundles = _bundleManager.getAllKnownBundles();
      for(BundleItem bundle : bundles) {
        if(bundle.getId().equals(bundleId)) {
          Date targetDate = bundle.getServiceDateFrom().getAsDate();
          _bundleManager.setTime(targetDate);
          break;
        }
      }
    }
      
    _bundleManager.changeBundle(bundleId);
    
    return new ModelAndView("redirect:/bundles.do");
  }
}
