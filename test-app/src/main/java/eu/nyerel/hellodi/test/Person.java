package eu.nyerel.hellodi.test;

import javax.inject.Named;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Named
public class Person implements Talkative {

    public void sayHello() {
        System.out.println("Hello from person bean!");
    }

}
