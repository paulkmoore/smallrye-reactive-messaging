package inbound;

import org.apache.camel.component.file.GenericFile;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;

@ApplicationScoped
public class CamelFileConsumer {

    @Incoming("files")
    public void consume(GenericFile<File> gf) {
        File file = gf.getFile();
        // process the file

    }

}
