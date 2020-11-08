package eu.nyerel.hellodi.test;

import javax.inject.Named;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Named
public class OtherBean {

    private final Person person;

    public OtherBean(Person person) {
        this.person = person;
    }

    public Person getPerson() {
        return person;
    }

}