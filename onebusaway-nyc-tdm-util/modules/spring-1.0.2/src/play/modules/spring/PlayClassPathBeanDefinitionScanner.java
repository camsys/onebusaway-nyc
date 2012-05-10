package play.modules.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

public class PlayClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner
{
	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
		this .resourcePatternResolver);

	/**
	 * The constructor, which just passed on to the parent class.
	 *
	 * @param registry
	 */
	public PlayClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry)
	{
		super(registry);
	}

	/**
	 * The override, which searches through the play framework's classes, instead of using
	 * files (as Spring is trained to do).
	 */
	@Override
	public Set<BeanDefinition> findCandidateComponents(String basePackage)
	{
		Logger.debug("Finding candidate components with base package: " + basePackage);

		Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();

		try
		{
			for (ApplicationClass appClass : Play.classes.all())
			{
				if (appClass.name.startsWith(basePackage))
				{
					Logger.debug("Scanning class: " + appClass.name);

                    AbstractResource res = null;
                    if (System.clearProperty("precompile") == null)  {
                       File f = Play.getFile("precompiled/java/" + (appClass.name.replace(".", "/")) + ".class");
                       res = new InputStreamResource(new FileInputStream(f));
                    } else {
                       res = new ByteArrayResource(appClass.enhance());
                    }
    				MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(res);

    				if (isCandidateComponent(metadataReader))
    				{
    					ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
    					sbd.setSource(res);
    					if (isCandidateComponent(sbd))
    					{
    						candidates.add(sbd);
    					}
    				}
				}
				else
				{
					Logger.trace("Skipped class: " + appClass.name + " -- wrong base package");
				}
			}
		}
		catch (IOException ex)
		{
			throw new BeanDefinitionStoreException(
					"I/O failure during classpath scanning", ex);
		}
		return candidates;
	}
}
