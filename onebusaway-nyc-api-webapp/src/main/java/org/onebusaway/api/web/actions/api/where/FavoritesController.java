package org.onebusaway.api.web.actions.api.where;


import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.where.CookieData;
import org.onebusaway.api.model.where.FavoritesCookieData;
import org.onebusaway.api.web.mapping.formatting.CookieDataFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ResponseBean> index(@CookieValue(name=cookieId, required = false) String cookieValue){
        CookieData obaCookieValue = CookieDataFormatter.toObj(cookieValue, FavoritesCookieData.class);
        if(obaCookieValue==null)obaCookieValue=new FavoritesCookieData();
        return new ResponseEntity<>(new ResponseBean(V2, ResponseCodes.RESPONSE_OK,
                "OK", obaCookieValue), HttpStatus.valueOf(ResponseCodes.RESPONSE_OK));
    }

    @GetMapping("{operation}/{type}/{id}")
    public ResponseEntity<ResponseBean> index(@PathVariable("operation") String operation,
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
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return new ResponseEntity<>(new ResponseBean(V2, ResponseCodes.RESPONSE_OK,
                "OK", null), HttpStatus.valueOf(ResponseCodes.RESPONSE_OK));
    }
}
