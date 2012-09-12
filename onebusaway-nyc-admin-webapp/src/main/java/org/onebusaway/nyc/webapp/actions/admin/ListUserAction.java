package org.onebusaway.nyc.webapp.actions.admin;

import org.onebusaway.nyc.admin.model.ui.UserDetail;
import org.onebusaway.presentation.impl.NextActionSupport;
import org.onebusaway.users.client.model.UserBean;
import org.onebusaway.users.model.User;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.model.UserIndexKey;
import org.onebusaway.users.model.UserProperties;
import org.onebusaway.users.model.properties.UserPropertiesV2;
import org.onebusaway.users.services.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListUserAction extends NextActionSupport {
private static Logger _log = LoggerFactory.getLogger(ListUserAction.class);
    private UserService userService;
    
    public List<UserDetail> getExistingUsers() {
      List<UserDetail> users = new ArrayList<UserDetail>();
      List<Integer> userIds = userService.getAllUserIds();

      for (Integer userId : userIds) {

        User user = userService.getUserForId(userId);
        if (user != null) {
          users.add(createUserDetail(user));
          _log.info("found user=" + userId + " with id=" + user.getId()); 
        } else {
          _log.warn("no user found for id=" + userId);
        }
        
      }
      return users;
    }
    
  private UserDetail createUserDetail(User user) {
    UserDetail detail = new UserDetail();
    // TODO Auto-generated method stub
    Set<UserIndex> indices = user.getUserIndices();
    for (UserIndex index : indices) {
      _log.info("found id=" + index.getId() + " for user=" + user);
      UserIndexKey key = index.getId();
      _log.info("found type=" + key.getType() + " with value=" + key.getValue());
      if (key.getType().equals("username")) {
        detail.setUsername(key.getValue());
      }
    }
    UserBean userBean = userService.getUserAsBean(user);
    detail.setAdmin(userBean.isAdmin());
    return detail;
  }

  /**
	 * Injects {@link UserService}
	 * @param userService the userService to set
	 */
	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

}
