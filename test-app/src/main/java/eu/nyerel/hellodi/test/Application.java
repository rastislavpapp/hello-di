package eu.nyerel.hellodi.test;

import eu.nyerel.hellodi.test.inner.MultipleConstructorsBean;

import javax.inject.Named;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Named
public class Application {

    private final Talkative person;
    private final Talkative animal;
    private final Talkative someBean;
    private final MultipleConstructorsBean multipleConstructorsBean;

    public Application(@Named("person") Talkative person,
                       @Named("animal") Talkative animal,
                       @Named("someBean") Talkative someBean,
                       MultipleConstructorsBean multipleConstructorsBean) {
        this.person = person;
        this.animal = animal;
        this.someBean = someBean;
        this.multipleConstructorsBean = multipleConstructorsBean;
    }

    public static void main(String ... args) {
        Injector.inject().run();
    }

    public void run() {
        person.sayHello();
        animal.sayHello();
        someBean.sayHello();
        multipleConstructorsBean.test();
    }

}