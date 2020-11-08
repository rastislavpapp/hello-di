package eu.nyerel.hellodi.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Value
@Builder
public class BeanWiring {

    List<Bean> beans;
    List<Bean> rootBeans;

}
