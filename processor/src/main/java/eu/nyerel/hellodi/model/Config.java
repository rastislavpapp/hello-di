package eu.nyerel.hellodi.model;

import lombok.Builder;
import lombok.Value;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Value
@Builder
public class Config {

    String injectorName;
    String injectorPackage;
    boolean debug;

}
