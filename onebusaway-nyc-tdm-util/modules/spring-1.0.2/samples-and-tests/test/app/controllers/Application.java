package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;
import  play.modules.spring.*;

public class Application extends Controller {

    public static void index() {
        Test test = Spring.getBeanOfType(Test.class);
        renderText(test.name);
    }

}