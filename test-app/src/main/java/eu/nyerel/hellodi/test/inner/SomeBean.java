package eu.nyerel.hellodi.test.inner;

import eu.nyerel.hellodi.test.OtherBean;
import eu.nyerel.hellodi.test.Person;
import eu.nyerel.hellodi.test.Talkative;

import javax.inject.Named;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Named
public class SomeBean implements Talkative {

    private final OtherBean otherBean;
    private final Person person;

    public SomeBean(OtherBean otherBean, Person person) {
        this.otherBean = otherBean;
        this.person = person;
    }

    public OtherBean getOtherBean() {
        return otherBean;
    }

    public Person getPerson() {
        return person;
    }

    @Override
    public void sayHello() {
        System.out.println("Hello from SomeBean");
    }

}