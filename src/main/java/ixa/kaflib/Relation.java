package ixa.kaflib;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/** Class for representing relations between entities and/or features. */
public class Relation {
    
    private AnnotationContainer annotationContainer;

    /* Relation's ID (required) */
    private String id;

    /* From (required) */
    private Relational from;

    /* To (required) */
    private Relational to;

    /* Confidence (optional) */
    private float confidence;

    Relation (AnnotationContainer annotationContainer, String id, Relational from, Relational to) {
	this.annotationContainer = annotationContainer;
	this.id = id;
	this.from = from;
	this.to = to;
	this.confidence = -1.0f;
    }

    Relation(Relation relation, AnnotationContainer annotationContainer, HashMap<String, Relational> relational) {
	this.annotationContainer = annotationContainer;
	this.id = id;
	if (relation.from != null) {
	    this.from = relational.get(relation.from.getId());
	    if (this.from == null) {
		throw new IllegalStateException("Couldn't find relational " + relation.from.getId() + " when copying " + relation.getId());
	    }
	}
	if (relation.to != null) {
	    this.to = relational.get(relation.to.getId());
	    if (this.to == null) {
		throw new IllegalStateException("Couldn't find relational " + relation.to.getId() + " when copying " + relation.getId());
	    }
	}
	this.confidence = relation.confidence;
    }

    public String getId() {
	return this.id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public Relational getFrom() {
        return this.from;
    }

    public void setFrom(Relational obj) {
	this.from = obj;
    }

    public Relational getTo() {
        return this.to;
    }

    public void setTo(Relational obj) {
	this.to = obj;
    }

    public boolean hasConfidence() {
	return confidence >= 0;
    }

    public float getConfidence() {
	if (confidence < 0) {
	    return 1.0f;
	}
	return confidence;
    }

    public void setConfidence(float confidence) {
	if ((confidence < 0.0f) || (confidence > 1.0f)) {
	    throw new IllegalStateException("Confidence's value in a relation must be >=0 and <=1. [0, 1].");
	}
	this.confidence = confidence;
    }

    public String getStr() {
	String str = "(" + this.from.getStr() + ", " + this.to.getStr() + ")";
	if (this.hasConfidence()) {
	    str += " [" + this.getConfidence() + "]";
	}
	return str;
    }
}
