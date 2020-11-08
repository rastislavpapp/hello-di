package eu.nyerel.hellodi.test.inner;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MultipleConstructorsBean {

    private final Another another;

    public MultipleConstructorsBean(String impossible) {
        throw new AssertionError("this should not be reachable");
    }

    @Inject
    public MultipleConstructorsBean(Another another) {
        this.another = another;
    }

    public void test() {
        System.out.println(another);
    }

}
