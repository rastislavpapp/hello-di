package eu.nyerel.hellodi;

import eu.nyerel.hellodi.core.InjectorClassGenerator;
import eu.nyerel.hellodi.model.Bean;
import eu.nyerel.hellodi.model.BeanWiring;
import eu.nyerel.hellodi.model.Config;
import eu.nyerel.hellodi.util.StringUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Named;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
@SupportedOptions({
        Processor.OPT_INJECTOR_NAME,
        Processor.OPT_INJECTOR_PACKAGE,
        Processor.OPT_DEBUG,
})
@SupportedAnnotationTypes({
        Processor.ANNOTATION_NAMED
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class Processor extends AbstractProcessor {

    static final String ANNOTATION_NAMED = "javax.inject.Named";
    static final String OPT_INJECTOR_NAME = "hellodi.injector.name";
    static final String OPT_INJECTOR_PACKAGE = "hellodi.injector.package";
    static final String OPT_DEBUG = "hellodi.debug";

    private static final String DEFAULT_INJECTOR_NAME = "Injector";
    private static final String DEFAULT_INJECTOR_PACKAGE = "eu.nyerel.hellodi";

    private final Map<String, TypeElement> beanTypesByName = new HashMap<>();
    private final Map<TypeElement, Set<String>> beanNamesByType = new HashMap<>();

    private Config config;
    private InjectorClassGenerator injectorClassGenerator;
    private AtomicInteger roundCounter = new AtomicInteger(0);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.config = parseConfig();
        this.injectorClassGenerator = new InjectorClassGenerator(config);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        int round = roundCounter.incrementAndGet();
        debug("Options: " + processingEnv.getOptions());
        debug("Start of round " + round + ": " + roundEnv);

        if (roundEnv.errorRaised()) {
            debug("Skipping, error was raised in one of previous rounds");
            return false;
        }

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            if (!ANNOTATION_NAMED.equals(annotation.getQualifiedName().toString())) {
                reportError("Unexpected annotation given to process - " + annotation);
                continue;
            }
            debug("Elements of '" + annotation + "': " + elements);
            registerBeans(getClassElements(elements));
        }

        if (roundEnv.processingOver()) {
            if (beanTypesByName.isEmpty()) {
                debug("No beans found, injector will not be created");
            } else {
                try {
                    debug("Writing injector for beans: " + beanTypesByName.keySet());
                    writeInjectorClass();
                } catch (IOException e) {
                    reportError("Error while creating injector file: " + e.getClass() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    private Set<TypeElement> getClassElements(Set<? extends Element> elements) {
        return elements.stream()
                .filter(e -> e.getKind() == ElementKind.CLASS)
                .map(e -> (TypeElement) e)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void registerBeans(Set<TypeElement> beans) {
        debug("Registering beans: " + beans);
        beans.forEach(b -> {
            Named namedAnnotation = b.getAnnotation(Named.class);
            String beanName;
            if (namedAnnotation != null && !namedAnnotation.value().isEmpty()) {
                beanName = namedAnnotation.value();
            } else {
                beanName = StringUtil.makeFirstLetterLowerCase(b.getSimpleName().toString());
            }
            registerBean(beanName, b);
        });
    }

    private void registerBean(String beanName, TypeElement beanType) {
        debug("Registering bean " + beanName + " of type " + beanType);
        beanNamesByType.computeIfAbsent(beanType, t -> new HashSet<>()).add(beanName);
        TypeElement existing = beanTypesByName.put(beanName, beanType);
        if (existing != null && !existing.equals(beanType)) {
            reportError("Detected multiple beans with the same name - " + beanName);
        }
    }

    private void writeInjectorClass() throws IOException {
        BeanWiring beanWiring = createBeanWiring();
        String injectorPackage = determineInjectorPackageName(beanWiring);
        JavaFileObject jfo = createInjectorSourceFile(injectorPackage);
        try (PrintWriter out = new PrintWriter(jfo.openWriter())) {
            String classContent = injectorClassGenerator.generate(beanWiring, injectorPackage);
            out.print(classContent);
        }
    }

    private String determineInjectorPackageName(BeanWiring beanWiring) {
        List<Bean> rootBeans = beanWiring.getRootBeans();
        if (config.getInjectorPackage() != null) {
            debug("Using injector package '" + config.getInjectorPackage() + "' from config");
            return config.getInjectorPackage();
        } else {
            long rootBeanPackageCount = rootBeans.stream().map(Bean::getPackageName).distinct().count();
            if (rootBeanPackageCount == 1) {
                Bean rootBean = rootBeans.get(0);
                String rootBeanPackage = rootBean.getPackageName();
                debug("Using injector package '" + rootBeanPackage + "' because there was a single root bean " + rootBean.getName());
                return rootBeanPackage;
            } else {
                String reason;
                if (rootBeanPackageCount == 0) {
                    reason = "there was no root bean";
                } else {
                    reason = "there were multiple root beans in different packages - " + rootBeans;
                }
                debug("Using default injector package '" + DEFAULT_INJECTOR_PACKAGE + "', " + reason);
                return DEFAULT_INJECTOR_PACKAGE;
            }
        }
    }

    private JavaFileObject createInjectorSourceFile(String injectorPackage) throws IOException {
        Element[] originatingElements = beanTypesByName.values().toArray(new Element[0]);
        return processingEnv.getFiler().createSourceFile(injectorPackage + "." + config.getInjectorName(), originatingElements);
    }

    private Config parseConfig() {
        Map<String, String> opts = processingEnv.getOptions();
        return Config.builder()
                .injectorName(opts.getOrDefault(OPT_INJECTOR_NAME, DEFAULT_INJECTOR_NAME))
                .injectorPackage(opts.get(OPT_INJECTOR_PACKAGE))
                .debug("true".equals(opts.get(OPT_DEBUG)))
                .build();
    }

    private BeanWiring createBeanWiring() {
        Map<String, Bean> beansByName = new HashMap<>();
        Map<String, Set<String>> beanDependencyMap = new HashMap<>();
        for (Map.Entry<String, TypeElement> entry : beanTypesByName.entrySet()) {
            TypeElement type = entry.getValue();
            Bean bean = Bean.builder()
                    .name(entry.getKey())
                    .type(type.getQualifiedName().toString())
                    .packageName(type.getEnclosingElement().toString())
                    .dependencies(computeDependencies(type))
                    .build();
            beansByName.put(bean.getName(), bean);
            beanDependencyMap.put(bean.getName(), new HashSet<>(bean.getDependencies()));
        }
        Set<String> notYetCreated = new HashSet<>(beansByName.keySet());
        List<String> beanCreationOrder = new ArrayList<>();
        List<String> lastCreated = Collections.emptyList();
        while (!notYetCreated.isEmpty()) {

            List<String> canCreate = new ArrayList<>();
            for (String bean : notYetCreated) {
                Set<String> dependencies = beanDependencyMap.get(bean);
                if (dependencies.isEmpty()) {
                    canCreate.add(bean);
                } else {
                    boolean satisfied = dependencies.stream().allMatch(dep -> {
                        return canCreate.contains(dep) || beanCreationOrder.contains(dep);
                    });
                    if (satisfied) {
                        canCreate.add(bean);
                    }
                }
            }

            beanCreationOrder.addAll(canCreate);
            notYetCreated.removeAll(canCreate);

            if (canCreate.isEmpty()) {
                break;
            } else {
                lastCreated = canCreate;
            }

        }

        notYetCreated.forEach(bean -> {
            String type = beansByName.get(bean).getType();
            reportError("Unable to satisfy dependencies of bean '" + bean + "' of type " + type + ". " +
                    "This might be a result of an incremental compilation, in which case not all the necessary " +
                    "classes are passed to the annotation processor. This is not supported yet, and you need to perform " +
                    "a full rebuild of the project. Otherwise there might be cyclic dependencies.");
        });

        return BeanWiring.builder()
                .beans(toBeans(beanCreationOrder, beansByName))
                .rootBeans(findRootBeans(lastCreated, beansByName))
                .build();
    }

    private List<Bean> findRootBeans(List<String> possibleRootBeanNames, Map<String, Bean> beansByName) {
        Set<String> rootBeanNames = new HashSet<>(possibleRootBeanNames);
        possibleRootBeanNames.stream()
                .map(beansByName::get)
                .forEach(b -> rootBeanNames.removeAll(b.getDependencies()));
        debug("Root beans: " + rootBeanNames);
        return toBeans(rootBeanNames, beansByName);
    }

    private List<Bean> toBeans(Collection<String> beanNames, Map<String, Bean> beansByName) {
        return beanNames.stream().map(beansByName::get).collect(Collectors.toList());
    }

    private List<String> computeDependencies(TypeElement value) {
        ExecutableElement constructor = findConstructorToUseForInjection(value);
        if (constructor != null) {
            List<String> dependencies = new ArrayList<>();
            for (VariableElement parameter : constructor.getParameters()) {
                Named named = parameter.getAnnotation(Named.class);
                if (named != null && !named.value().isEmpty()) {
                    dependencies.add(named.value());
                } else {
                    List<String> candidateBeans = findInjectionCandidatesForType(parameter);
                    if (candidateBeans.size() == 1) {
                        dependencies.add(candidateBeans.get(0));
                    } else {
                        String dep = parameter.getSimpleName().toString();
                        if (!beanTypesByName.containsKey(dep)) {
                            reportError("Unable to wire '" + dep + "' into " + constructor + ": no bean named '" + dep + "' found.");
                            return Collections.emptyList();
                        }
                        dependencies.add(dep);
                    }
                }
            }
            return dependencies;
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> findInjectionCandidatesForType(VariableElement param) {
        TypeElement type = (TypeElement) ((DeclaredType) param.asType()).asElement();
        Set<String> names = beanNamesByType.get(type);
        if (names == null) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(names);
        }
    }

    private ExecutableElement findConstructorToUseForInjection(TypeElement value) {
        List<? extends Element> constructors = value.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .collect(Collectors.toList());
        if (constructors.size() == 1) {
            return (ExecutableElement) constructors.get(0);
        } else {
            if (!constructors.isEmpty()) {
                List<? extends Element> injectableConstructors = constructors.stream()
                        .filter(c -> c.getAnnotation(Inject.class) != null)
                        .collect(Collectors.toList());
                if (injectableConstructors.isEmpty()) {
                    reportError("Multiple constructors on " + value + ", but no " + Inject.class.getName() + " found.");
                } else {
                    if (injectableConstructors.size() > 1) {
                        reportError("Multiple constructors on " + value + " annotated with " + Inject.class.getName() +
                                ". There must be at most one.");
                    } else {
                        return (ExecutableElement) injectableConstructors.get(0);
                    }
                }
            } else {
                reportError("No constructor found on " + value + ", this should not happen");
            }
        }
        return null;
    }

    private void reportError(String message) {
        reportMessage(Diagnostic.Kind.ERROR, message);
    }

    private void debug(String message) {
        if (config.isDebug()) {
            reportMessage(Diagnostic.Kind.NOTE, message);
        }
    }

    private void reportMessage(Diagnostic.Kind kind, String message) {
        processingEnv.getMessager().printMessage(kind, this.getClass().getName() + ": " + message);
    }

}