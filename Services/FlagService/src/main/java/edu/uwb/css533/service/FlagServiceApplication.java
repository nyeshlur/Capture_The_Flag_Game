package edu.uwb.css533.service;

import edu.uwb.css533.service.db.FlagDao;
import edu.uwb.css533.service.resources.FlagResource;
import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;

public class FlagServiceApplication extends Application<FlagServiceConfiguration> {

    public static void main(final String[] args) throws Exception {
        new FlagServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "FlagService";
    }

    @Override
    public void initialize(final Bootstrap<FlagServiceConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final FlagServiceConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");
        final FlagDao flag_dao = jdbi.onDemand(FlagDao.class);
        environment.jersey().register(new FlagResource(jdbi, flag_dao));
    }

}
