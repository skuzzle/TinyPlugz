package de.skuzzle.tinyplugz.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.util.Require;

public class PluginSourceBuilderImplListFilesTest {

    private final PluginSourceBuilderImpl subject;
    private final Path root;

    public PluginSourceBuilderImplListFilesTest() {
        this.subject = new PluginSourceBuilderImpl();
        this.root = getTestRoot();
        Require.state(Files.exists(this.root), "dir '%s' does not exist", this.root);
    }

    private Path getTestRoot() {
        final String className = getClass().getPackage().getName();
        final String relative = className.replace(".", File.separator);
        final String root = "src/test/resources";
        return Paths.get(root, relative);
    }

    @Test
    public void testListFiles() throws Exception {
        this.subject.addAllPluginJars(this.root);
        final PluginSource source = this.subject.createSource();
        assertEquals(2, source.getPluginURLs().count());
    }

    @Test
    public void testListFilesWithFilter() throws Exception {
        this.subject.addAllPluginJars(this.root,
                path -> path.getFileName().toString().contains("1"));
        final PluginSource source = this.subject.createSource();
        assertEquals(1, source.getPluginURLs().count());
    }
}
