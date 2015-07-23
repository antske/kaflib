package ixa.kaflib;

import ixa.kaflib.NAFDefinition.AnnotationType;

import java.io.Serializable;
import java.util.Map;
import java.util.List;


public abstract class Annotation implements Serializable {
    
    abstract Map<AnnotationType, List<Annotation>> getReferencedAnnotations();
    
}
