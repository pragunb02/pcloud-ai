package com.pcloudai.backend

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.pcloudai.backend.api.AuthResource
import com.pcloudai.backend.api.FileResource
import com.pcloudai.backend.api.HealthResource
import com.pcloudai.backend.api.MetricsResource
import com.pcloudai.backend.api.UserResource
import com.pcloudai.backend.auth.JwtAuthFilter
import com.pcloudai.backend.auth.UserPrincipal
import com.pcloudai.backend.core.domain.File
import com.pcloudai.backend.core.domain.User
import com.pcloudai.backend.core.domain.UserSettings
import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.hibernate.HibernateBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature
import org.slf4j.LoggerFactory
import java.util.EnumSet
import javax.servlet.DispatcherType

class PCloudApplication : Application<PCloudConfiguration>() {
    private val logger = LoggerFactory.getLogger(PCloudApplication::class.java)

    private val hibernateBundle = object : HibernateBundle<PCloudConfiguration>(
        User::class.java,
        File::class.java,
        UserSettings::class.java
    ) {
        override fun getDataSourceFactory(configuration: PCloudConfiguration): DataSourceFactory {
            return configuration.database
        }
    }

    override fun initialize(bootstrap: Bootstrap<PCloudConfiguration>) {
        logger.info("Initializing PCloud Application")
        bootstrap.objectMapper.registerModule(KotlinModule.Builder().build())
        bootstrap.addBundle(hibernateBundle)
    }

    override fun run(config: PCloudConfiguration, environment: Environment) {
        logger.info("Starting PCloud Application with Guice")

        // Create the Guice injector from our module. It's now a factory for all our objects.
        val injector = Guice.createInjector(PCloudGuiceModule(config, environment, hibernateBundle))

        environment.jersey().register(MultiPartFeature::class.java)

        configureCors(environment, config)
        configureAuthentication(environment, injector)
        registerResources(environment, injector)

        logger.info("PCloud Application started successfully")
    }

    private fun configureCors(environment: Environment, config: PCloudConfiguration) {
        logger.info("Configuring CORS with allowed origins: ${config.cors.allowedOrigins}")
        val cors = environment.servlets().addFilter("CORS", CrossOriginFilter::class.java)
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, config.cors.allowedOrigins)
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS,HEAD,PATCH")
        cors.setInitParameter(
            CrossOriginFilter.ALLOWED_HEADERS_PARAM,
            "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,Content-Disposition"
        )
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true")
        cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*")
    }

    private fun configureAuthentication(environment: Environment, injector: Injector) {
        logger.info("Configuring authentication")

        // Get the fully constructed JwtAuthFilter instance from Guice.
        val authFilter = injector.getInstance(JwtAuthFilter::class.java)

        // Register the JWT authentication filter with the Dropwizard.
        environment.jersey().register(AuthDynamicFeature(authFilter))
        environment.jersey().register(RolesAllowedDynamicFeature::class.java)
        environment.jersey().register(AuthValueFactoryProvider.Binder(UserPrincipal::class.java))
    }

    private fun registerResources(environment: Environment, injector: Injector) {
        logger.info("Registering REST resources")

        // Ask Guice for each fully constructed resource and register it with Jersey.
        environment.jersey().register(injector.getInstance(AuthResource::class.java))
        environment.jersey().register(injector.getInstance(FileResource::class.java))
        environment.jersey().register(injector.getInstance(UserResource::class.java))
        environment.jersey().register(injector.getInstance(MetricsResource::class.java))
        environment.jersey().register(injector.getInstance(HealthResource::class.java))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PCloudApplication().run()
        }
    }
}
