package eu.nyerel.hellodi.test;

import javax.inject.Named;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Named
public class Animal implements Talkative {

    @Override
    public void sayHello() {
        System.out.println("mooo");
    }

}
