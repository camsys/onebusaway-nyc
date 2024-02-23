package org.onebusaway.api.web.actions.api.where;


import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.where.CookieData;
import org.onebusaway.api.model.where.FavoritesCookieData;
import org.onebusaway.api.web.mapping.formatting.CookieDataFormatter;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/where/favorites")
public class FavoritesController {

    private static final long serialVersionUID = 1L;

    private static final int V2 = 1;

    public static final String cookieId = "favorites";

    @GetMapping("/get-all")
    public ResponseBean index(@CookieValue(name=cookieId, required = false) String cookieValue){
        CookieData obaCookieValue = CookieDataFormatter.toObj(cookieValue, FavoritesCookieData.class);
        if(obaCookieValue==null)obaCookieValue=new FavoritesCookieData();
        return new ResponseBean(V2,ResponseCodes.RESPONSE_OK,
                "OK", obaCookieValue);
    }

    @GetMapping("{operation}/{type}/{id}")
    public ResponseBean index(@PathVariable("operation") String operation,
                              @PathVariable("type") String type,
                              @PathVariable("id") String id,
                              HttpServletResponse response,
                              @CookieValue(name=cookieId, required = false) String  cookieValue){
        FavoritesCookieData favoritesCookieValue= (FavoritesCookieData) CookieDataFormatter.toObj(
                cookieValue,FavoritesCookieData.class);
        if(operation.equalsIgnoreCase("add")){
            if(type.equalsIgnoreCase("route")) {
                favoritesCookieValue.routes.add(id);
            }
            if(type.equalsIgnoreCase("stop")) {
                favoritesCookieValue.stops.add(id);
            }
        }
        if(operation.equalsIgnoreCase("remove")){
            if(type.equalsIgnoreCase("route")) {
                favoritesCookieValue.routes.remove(id);
            }
            if(type.equalsIgnoreCase("stop")) {
                favoritesCookieValue.stops.remove(id);
            }
        }
        Cookie cookie = new Cookie(cookieId, favoritesCookieValue.toString());
        cookie.setPath("/");
        response.addCookie(cookie);
        return new ResponseBean(V2,ResponseCodes.RESPONSE_OK,
                "OK", null);
    }
}
