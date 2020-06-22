//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0
//DEPS io.quarkus:quarkus-registry-descriptor:1.0.0.Beta8
//DEPS io.quarkus:quarkus-platform-descriptor-json:1.5.1.Final

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.quarkus.registry.RepositoryIndexer;
import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.registry.builder.RegistryModelBuilder;
import io.quarkus.registry.impl.DefaultArtifactResolver;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "index", mixinStandardHelpOptions = true, version = "0.1",
        description = "Index Catalog")
class index implements Callable<Integer> {

    @Parameters(index = "0", description = "The repository path to index")
    private Path repositoryPath;

    @Parameters(index = "1", description = "The output file")
    private File outputFile;

    public static void main(String... args) {
        int exitCode = new CommandLine(new index()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        Repository repository = Repository.parse(repositoryPath, mapper);
        RepositoryIndexer indexer = new RepositoryIndexer(new DefaultArtifactResolver(mapper));
        RegistryModelBuilder builder = new RegistryModelBuilder();
        indexer.index(repository, builder);
        // Make sure the parent directory exists
        File parentFile = outputFile.getAbsoluteFile().getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        mapper.writeValue(outputFile, builder.build());
        return 0;
    }
}
