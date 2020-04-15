package io.quarkus.extension.catalog.neo4j;

import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.dependencies.Extension;
import io.quarkus.extension.catalog.neo4j.nodes.Platform;
import io.quarkus.extension.catalog.neo4j.nodes.QuarkusCore;
import io.quarkus.extensions.catalog.Indexer;
import io.quarkus.extensions.catalog.model.Repository;
import io.quarkus.extensions.catalog.spi.IndexVisitor;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

public class Neo4jExample {
    public static void main(String[] args) throws Exception {
        Repository repository = Repository.parse(Paths.get("../playground/repository"), new ObjectMapper());
        Indexer indexer = new Indexer();
        try (Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "password"))) {
            SessionFactory sessionFactory = new SessionFactory(new BoltDriver(driver), Platform.class.getPackage().getName());
            final Session session = sessionFactory.openSession();
            try (Transaction transaction = session.beginTransaction()) {
                indexer.index(repository, new IndexVisitor() {
                    @Override
                    public void visitPlatform(QuarkusPlatformDescriptor bom) {
                        QuarkusCore core = new QuarkusCore();
                        core.version = bom.getQuarkusVersion();
                        Platform platform = new Platform(bom.getBomGroupId(), bom.getBomArtifactId(), bom.getBomVersion(), core);
                        for (Extension extension :bom.getExtensions()) {
                            io.quarkus.extension.catalog.neo4j.nodes.Extension ext =
                                    new io.quarkus.extension.catalog.neo4j.nodes.Extension(extension.getGroupId(), extension.getArtifactId(), extension.getVersion(), core);
                            platform.addExtension(ext);
                        }
                        session.save(platform);

                    }

                    @Override
                    public void visitExtension(Extension extension, String quarkusCore) {
                        QuarkusCore core = new QuarkusCore();
                        core.version = quarkusCore;
                        io.quarkus.extension.catalog.neo4j.nodes.Extension ext =
                                new io.quarkus.extension.catalog.neo4j.nodes.Extension(extension.getGroupId(), extension.getArtifactId(), extension.getVersion(), core);
                        session.save(ext);
                    }
                });

                transaction.commit();
            }
       }
    }
}
