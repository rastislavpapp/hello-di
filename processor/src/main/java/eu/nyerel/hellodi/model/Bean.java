package eu.nyerel.hellodi.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@Value
@Builder
public class Bean {

    String type;
    String name;
    @ToString.Exclude
    String packageName;
    @ToString.Exclude
    List<String> dependencies;

}
