package play.modules.spring;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import org.xml.sax.InputSource;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.PlayException;
import play.inject.BeanSource;
import play.inject.Injector;
import play.vfs.VirtualFile;

public class SpringPlugin extends PlayPlugin implements BeanSource {
    /**
     * Component scanning constants.
     */
    private static final String PLAY_SPRING_COMPONENT_SCAN_FLAG = "play.spring.component-scan";
    private static final String PLAY_SPRING_COMPONENT_SCAN_BASE_PACKAGES = "play.spring.component-scan.base-packages";
    private static final String PLAY_SPRING_ADD_PLAY_PROPERTIES = "play.spring.add-play-properties";
    private static final String PLAY_SPRING_NAMESPACE_AWARE = "play.spring.namespace-aware";

    public static GenericApplicationContext applicationContext;
    private long startDate = 0;

    @Override
    public void detectChange() {
        if (Play.mode == Mode.DEV) {
            VirtualFile appRoot = VirtualFile.open(Play.applicationPath);
            long mod = appRoot.child("conf/application-context.xml").lastModified();
            if (mod > startDate) {
                throw new RuntimeException("conf/application-context.xml has changed");
            }
        }
    }

    @Override
    public void onApplicationStop() {
        if (applicationContext != null) {
            Logger.debug("Closing Spring application context");
            applicationContext.close();
        }
    }

    @Override
    public void onApplicationStart() {
        URL url = Play.classloader.getResource(Play.id + ".application-context.xml");
        if (url == null) {
            url = Play.classloader.getResource("application-context.xml");
        }
        if (url != null) {
            InputStream is = null;
            try {
                Logger.debug("Starting Spring application context");
                applicationContext = new GenericApplicationContext();
                applicationContext.setClassLoader(Play.classloader);
                XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(applicationContext);
                if (Play.configuration.getProperty(PLAY_SPRING_NAMESPACE_AWARE,
                                                   "false").equals("true")) {
                    xmlReader.setNamespaceAware(true);
                }
                xmlReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);

                if (Play.configuration.getProperty(PLAY_SPRING_ADD_PLAY_PROPERTIES,
                                                   "true").equals("true")) {
                    Logger.debug("Adding PropertyPlaceholderConfigurer with Play properties");
                    PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
                    configurer.setProperties(Play.configuration);
                    applicationContext.addBeanFactoryPostProcessor(configurer);
                } else {
                    Logger.debug("PropertyPlaceholderConfigurer with Play properties NOT added");
                }
                //
                //	Check for component scan 
                //
                boolean doComponentScan = Play.configuration.getProperty(PLAY_SPRING_COMPONENT_SCAN_FLAG, "false").equals("true");
                Logger.debug("Spring configuration do component scan: " + doComponentScan);
                if (doComponentScan) {
                    ClassPathBeanDefinitionScanner scanner = new PlayClassPathBeanDefinitionScanner(applicationContext);
                    String scanBasePackage = Play.configuration.getProperty(PLAY_SPRING_COMPONENT_SCAN_BASE_PACKAGES, "");
                    Logger.debug("Base package for scan: " + scanBasePackage);
                    Logger.debug("Scanning...");
                    scanner.scan(scanBasePackage.split(","));
                    Logger.debug("... component scanning complete");
                }

                is = url.openStream();
                xmlReader.loadBeanDefinitions(new InputSource(is));
                ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(Play.classloader);
                try {
                    applicationContext.refresh();
                    startDate = System.currentTimeMillis();
                } catch (BeanCreationException e) {
                    Throwable ex = e.getCause();
                    if (ex instanceof PlayException) {
                        throw (PlayException) ex;
                    } else {
                        throw e;
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            } catch (IOException e) {
                Logger.error(e, "Can't load spring config file");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Logger.error(e, "Can't close spring config file stream");
                    }
                }
            }
        }
        Injector.inject(this);
    }

    public <T> T getBeanOfType(Class<T> clazz) {
        Map<String, T> beans = applicationContext.getBeansOfType(clazz);
        if (beans.size() == 0) {
            return null;
        }
        return beans.values().iterator().next();
    }
}
