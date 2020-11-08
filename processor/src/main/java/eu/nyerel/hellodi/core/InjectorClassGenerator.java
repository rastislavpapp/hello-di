package eu.nyerel.hellodi.core;

import eu.nyerel.hellodi.model.Bean;
import eu.nyerel.hellodi.model.BeanWiring;
import eu.nyerel.hellodi.model.Config;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class InjectorClassGenerator {

    private final Config config;

    public String generate(BeanWiring beanWiring, String packageName) {
        List<Bean> beans = beanWiring.getBeans();
        List<Bean> rootBeans = beanWiring.getRootBeans();
        StringBuilder sb = new StringBuilder();
        //@formatter:off
        sb      .append("package ").append(packageName).append(";\n\n")
                .append("import java.util.HashMap;\n")
                .append("import java.util.Map;\n\n")
                .append("public class ").append(config.getInjectorName()).append(" {\n\n");
        String beanCreationFragment = createBeanCreationFragment(beans);
        if (rootBeans.size() == 1) {
            Bean rootBean = rootBeans.get(0);String rootBeanType = rootBean.getType();
            sb  .append("    public static " + rootBeanType + " inject() {\n")
                .append("        // bean creation\n")
                .append(beanCreationFragment)
                .append("\n")
                .append("        return " + rootBean.getName() + ";\n")
                .append("    }\n\n");
        }
        sb      .append("    public static <T> T inject(Class<T> appClass) {\n")
                .append("        // bean creation\n")
                .append(beanCreationFragment)
                .append("\n")
                .append("        // beans mapped by type\n")
                .append("        Map<Class<?>, Object> beanTypeMap = new HashMap<>();\n");
        beans.forEach(b -> {
            sb  .append("        ").append(String.format("beanTypeMap.put(%s.class, %s);\n", b.getType(), b.getName()));
        });
        sb      .append("\n")
                .append("        return (T) beanTypeMap.get(appClass);\n")
                .append("    }\n\n")
                .append("}\n");
        //@formatter:on
        return sb.toString();
    }

    private String createBeanCreationFragment(List<Bean> beans) {
        StringBuilder sb = new StringBuilder();
        beans.forEach(bean -> {
            sb.append("        ").append(String.format("%s %s = new %s(%s);\n",
                    bean.getType(),
                    bean.getName(),
                    bean.getType(),
                    String.join(", ", bean.getDependencies())));
        });
        return sb.toString();
    }

}