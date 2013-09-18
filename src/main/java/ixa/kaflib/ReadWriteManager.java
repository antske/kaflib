package ixa.kaflib;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Comment;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import org.jdom2.input.SAXBuilder;
import org.jdom2.JDOMException;
import org.jdom2.xpath.XPathExpression;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.io.File;
import java.io.Writer;
import java.io.Reader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.*;

/** Reads XML files in KAF format and loads the content in a KAFDocument object, and writes the content into XML files. */
class ReadWriteManager {
    
    /** Loads the content of a KAF file into the given KAFDocument object */
    static KAFDocument load(File file) throws IOException, JDOMException, KAFNotValidException {
	SAXBuilder builder = new SAXBuilder();
	Document document = (Document) builder.build(file);
	Element rootElem = document.getRootElement();
	return DOMToKAF(document);
    }

    /** Loads the content of a String in KAF format into the given KAFDocument object */
    static KAFDocument load(Reader stream) throws IOException, JDOMException, KAFNotValidException {
	SAXBuilder builder = new SAXBuilder();
	Document document = (Document) builder.build(stream);
	Element rootElem = document.getRootElement();
	return DOMToKAF(document);
    }

    /** Writes the content of a given KAFDocument to a file. */
    static void save(KAFDocument kaf, String filename) {
	try {
	    File file = new File(filename);
	    Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
	    out.write(kafToStr(kaf));
	    out.flush();
	} catch (Exception e) {
	    System.out.println("Error writing to file");
	}
    }

    /** Writes the content of a KAFDocument object to standard output. */
    static void print(KAFDocument kaf) {
	try {
	    Writer out = new BufferedWriter(new OutputStreamWriter(System.out, "UTF8"));
	    out.write(kafToStr(kaf));
	    out.flush();
	} catch (Exception e) {
	    System.out.println(e);
	}
    }

    /** Returns a string containing the XML content of a KAFDocument object. */
    static String kafToStr(KAFDocument kaf) {
	XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
	Document jdom = KAFToDOM(kaf);
	return out.outputString(jdom);
    }

    /** Loads a KAFDocument object from XML content in DOM format */
    private static KAFDocument DOMToKAF(Document dom) throws KAFNotValidException {
	HashMap<String, WF> wfIndex = new HashMap<String, WF>();
	HashMap<String, Term> termIndex = new HashMap<String, Term>();
	HashMap<String, Relational> relationalIndex = new HashMap<String, Relational>();
	Element rootElem = dom.getRootElement();
	String lang = getAttribute("lang", rootElem, Namespace.XML_NAMESPACE);
	String kafVersion = getAttribute("version", rootElem);
	KAFDocument kaf = new KAFDocument(lang, kafVersion);

	List<Element> rootChildrenElems = rootElem.getChildren();
	for (Element elem : rootChildrenElems) {
	    if (elem.getName().equals("nafHeader")) {
		List<Element> lpsElems = elem.getChildren("linguisticProcessors");
		for (Element lpsElem : lpsElems) {
		    String layer = getAttribute("layer", lpsElem);
		    List<Element> lpElems = lpsElem.getChildren();
		    for (Element lpElem : lpElems) {
			String name = getAttribute("name", lpElem);
			String timestamp = getOptAttribute("timestamp", lpElem);
			String version = getOptAttribute("version", lpElem);
			kaf.addLinguisticProcessor(layer, name, timestamp, version);
		    }
		}
		Element fileDescElem = elem.getChild("fileDesc");
		if (fileDescElem != null) {
		    KAFDocument.FileDesc fd = kaf.createFileDesc();
		    String author = getOptAttribute("author", fileDescElem);
		    if (author != null) {
			fd.author = author;
		    }
		    String title = getOptAttribute("title", fileDescElem);
		    if (title != null) {
			fd.title = title;
		    }
		    String creationtime = getOptAttribute("creationtime", fileDescElem);
		    if (creationtime != null) {
			fd.creationtime = creationtime;
		    }
		    String filename = getOptAttribute("filename", fileDescElem);
		    if (filename != null) {
			fd.filename = filename;
		    }
		    String filetype = getOptAttribute("filetype", fileDescElem);
		    if (filetype != null) {
			fd.filetype = filetype;
		    }
		    String pages = getOptAttribute("pages", fileDescElem);
		    if (pages != null) {
			fd.pages = Integer.parseInt(pages);
		    }
		}
		Element publicElem = elem.getChild("public");
		if (publicElem != null) {
		    String publicId = getAttribute("publicId", publicElem);
		    KAFDocument.Public pub = kaf.createPublic(publicId);
		    String uri = getOptAttribute("uri", publicElem);
		    if (uri != null) {
			pub.uri = uri;
		    }
		}
	    }
	    if (elem.getName().equals("text")) {
		List<Element> wfElems = elem.getChildren();
		for (Element wfElem : wfElems) {
		    String wid = getAttribute("wid", wfElem);
		    String wForm = wfElem.getText();
		    WF newWf = kaf.newWF(wid, wForm);
		    String wSent = getOptAttribute("sent", wfElem);
		    if (wSent != null) {
			newWf.setSent(Integer.valueOf(wSent));
		    }
		    String wPara = getOptAttribute("para", wfElem);
		    if (wPara != null) {
			newWf.setPara(Integer.valueOf(wPara));
		    }
		    String wPage = getOptAttribute("page", wfElem);
		    if (wPage != null) {
			newWf.setPage(Integer.valueOf(wPage));
		    }
		    String wOffset = getOptAttribute("offset", wfElem);
		    if (wOffset != null) {
			newWf.setOffset(Integer.valueOf(wOffset));
		    }
		    String wLength = getOptAttribute("length", wfElem);
		    if (wLength != null) {
			newWf.setLength(Integer.valueOf(wLength));
		    }
		    String wXpath = getOptAttribute("xpath", wfElem);
		    if (wXpath != null) {
			newWf.setXpath(wXpath);
		    }
		    wfIndex.put(newWf.getId(), newWf);
		}
	    }
	    if (elem.getName().equals("terms")) {
		List<Element> termElems = elem.getChildren();
		for (Element termElem : termElems) {
		    String tid = getAttribute("tid", termElem);
		    String type = getAttribute("type", termElem);
		    String lemma = getAttribute("lemma", termElem);
		    String pos = getAttribute("pos", termElem);
		    Element spanElem = termElem.getChild("span");
		    if (spanElem == null) {
			throw new IllegalStateException("Every term must contain a span element");
		    }
		    List<Element> termsWfElems = spanElem.getChildren("target");
		    Span<WF> span = kaf.newWFSpan();
		    for (Element termsWfElem : termsWfElems) {
			String wfId = getAttribute("id", termsWfElem);
			boolean isHead = isHead(termsWfElem);
			WF wf = wfIndex.get(wfId);
			if (wf == null) {
			    throw new KAFNotValidException("Wf " + wfId + " not found when loading term " + tid);
			}
			span.addTarget(wf, isHead);
		    }
		    Term newTerm = kaf.newTerm(tid, type, lemma, pos, span);
		    String tMorphofeat = getOptAttribute("morphofeat", termElem);
		    if (tMorphofeat != null) {
			newTerm.setMorphofeat(tMorphofeat);
		    }
		    String tHead = getOptAttribute("head", termElem);
		    String termcase = getOptAttribute("case", termElem);
		    if (termcase != null) {
			newTerm.setCase(termcase);
		    }
		    List<Element> sentimentElems = termElem.getChildren("sentiment");
		    if (sentimentElems.size() > 0) {
			Element sentimentElem = sentimentElems.get(0);
			Term.Sentiment newSentiment = kaf.newSentiment();
			String sentResource = getOptAttribute("resource", sentimentElem);
			if (sentResource != null) {
			    newSentiment.setResource(sentResource);
			}
			String sentPolarity = getOptAttribute("polarity", sentimentElem);
			if (sentPolarity != null) {
			    newSentiment.setPolarity(sentPolarity);
			}
			String sentStrength = getOptAttribute("strength", sentimentElem);
			if (sentStrength != null) {
			    newSentiment.setStrength(sentStrength);
			}
			String sentSubjectivity = getOptAttribute("subjectivity", sentimentElem);
			if (sentSubjectivity != null) {
			    newSentiment.setSubjectivity(sentSubjectivity);
			}
			String sentSentimentSemanticType = getOptAttribute("sentiment_semantic_type", sentimentElem);
			if (sentSentimentSemanticType != null) {
			    newSentiment.setSentimentSemanticType(sentSentimentSemanticType);
			}
			String sentSentimentModifier = getOptAttribute("sentiment_modifier", sentimentElem);
			if (sentSentimentModifier != null) {
			    newSentiment.setSentimentModifier(sentSentimentModifier);
			}
			String sentSentimentMarker = getOptAttribute("sentiment_marker", sentimentElem);
			if (sentSentimentMarker != null) {
			    newSentiment.setSentimentMarker(sentSentimentMarker);
			}
			String sentSentimentProductFeature = getOptAttribute("sentiment_product_feature", sentimentElem);
			if (sentSentimentProductFeature != null) {
			    newSentiment.setSentimentProductFeature(sentSentimentProductFeature);
			}
			newTerm.setSentiment(newSentiment);
		    }
		    List<Element> termsComponentElems = termElem.getChildren("component");
		    for (Element termsComponentElem : termsComponentElems) {
			String compId = getAttribute("id", termsComponentElem);
			boolean isHead = ((tHead != null) && tHead.equals(compId));
			String compLemma = getAttribute("lemma", termsComponentElem);
			String compPos = getAttribute("pos", termsComponentElem);
			Term.Component newComponent = kaf.newComponent(compId, newTerm, compLemma, compPos);
			List<Element> externalReferencesElems = termsComponentElem.getChildren("externalReferences");
			if (externalReferencesElems.size() > 0) {
			    List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			    newComponent.addExternalRefs(externalRefs);
			}
			newTerm.addComponent(newComponent, isHead);
		    }
		    List<Element> externalReferencesElems = termElem.getChildren("externalReferences");
		    if (externalReferencesElems.size() > 0) {
			List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			newTerm.addExternalRefs(externalRefs);
		    }
		    termIndex.put(newTerm.getId(), newTerm);
		}
	    }
	    if (elem.getName().equals("deps")) {
		List<Element> depElems = elem.getChildren();
		for (Element depElem : depElems) {
		    String fromId = getAttribute("from", depElem);
		    String toId = getAttribute("to", depElem);
		    Term from = termIndex.get(fromId);
		    if (from == null) {
			    throw new KAFNotValidException("Term " + fromId + " not found when loading Dep (" + fromId + ", " + toId + ")");
		    }
		    Term to = termIndex.get(toId);
		    if (to == null) {
			throw new KAFNotValidException("Term " + toId + " not found when loading Dep (" + fromId + ", " + toId + ")");
		    }
		    String rfunc = getAttribute("rfunc", depElem);
		    Dep newDep = kaf.newDep(from, to, rfunc);
		    String depcase = getOptAttribute("case", depElem);
		    if (depcase != null) {
			newDep.setCase(depcase);
		    }
		}
	    }
	    if (elem.getName().equals("chunks")) {
		List<Element> chunkElems = elem.getChildren();
		for (Element chunkElem : chunkElems) {
		    String chunkId = getAttribute("cid", chunkElem);
		    String headId = getAttribute("head", chunkElem);
		    Term chunkHead = termIndex.get(headId);
		    if (chunkHead == null) {
			throw new KAFNotValidException("Term " + headId + " not found when loading chunk " + chunkId);
		    }
		    String chunkPhrase = getAttribute("phrase", chunkElem);
		    Element spanElem = chunkElem.getChild("span");
		    if (spanElem == null) {
			throw new IllegalStateException("Every chunk must contain a span element");
		    }
		    List<Element> chunksTermElems = spanElem.getChildren("target");
		    Span<Term> span = kaf.newTermSpan();
		    for (Element chunksTermElem : chunksTermElems) {
			String termId = getAttribute("id", chunksTermElem);
			boolean isHead = isHead(chunksTermElem);
			Term targetTerm = termIndex.get(termId);
			if (targetTerm == null) {
			    throw new KAFNotValidException("Term " + termId + " not found when loading chunk " + chunkId);
			}
			span.addTarget(targetTerm, ((targetTerm == chunkHead) || isHead));
		    }
		    if (!span.hasTarget(chunkHead)) {
			throw new KAFNotValidException("The head of the chunk is not in it's span.");
		    }
		    Chunk newChunk = kaf.newChunk(chunkId, chunkPhrase, span);
		    String chunkCase = getOptAttribute("case", chunkElem);
		    if (chunkCase != null) {
			newChunk.setCase(chunkCase);
		    }
		}
	    }
	    if (elem.getName().equals("entities")) {
		List<Element> entityElems = elem.getChildren();
		for (Element entityElem : entityElems) {
		    String entId = getAttribute("eid", entityElem);
		    String entType = getAttribute("type", entityElem);
		    List<Element> referencesElem = entityElem.getChildren("references");
		    if (referencesElem.size() < 1) {
			throw new IllegalStateException("Every entity must contain a 'references' element");
		    }
		    List<Element> spanElems = referencesElem.get(0).getChildren();
		    if (spanElems.size() < 1) {
			throw new IllegalStateException("Every entity must contain a 'span' element inside 'references'");
		    }
		    List<Span<Term>> references = new ArrayList<Span<Term>>();
		    for (Element spanElem : spanElems) {
			Span<Term> span = kaf.newTermSpan();
			List<Element> targetElems = spanElem.getChildren();
			if (targetElems.size() < 1) {
			    throw new IllegalStateException("Every span in an entity must contain at least one target inside");  
			}
			for (Element targetElem : targetElems) {
			    String targetTermId = getAttribute("id", targetElem);
			    Term targetTerm = termIndex.get(targetTermId);
			    if (targetTerm == null) {
				throw new KAFNotValidException("Term " + targetTermId + " not found when loading entity " + entId);
			    }
			    boolean isHead = isHead(targetElem);
			    span.addTarget(targetTerm, isHead);
			}
			references.add(span);
		    }
		    Entity newEntity = kaf.newEntity(entId, entType, references);
		    List<Element> externalReferencesElems = entityElem.getChildren("externalReferences");
		    if (externalReferencesElems.size() > 0) {
			List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			newEntity.addExternalRefs(externalRefs);
		    }
		    relationalIndex.put(newEntity.getId(), newEntity);
		}
	    }
	    if (elem.getName().equals("coreferences")) {
		List<Element> corefElems = elem.getChildren();
		for (Element corefElem : corefElems) {
		    String coId = getAttribute("coid", corefElem);
		    List<Element> referencesElem = corefElem.getChildren("references");
		    if (referencesElem.size() < 1) {
			throw new IllegalStateException("Every coref must contain a 'references' element");
		    }
		    List<Element> spanElems = referencesElem.get(0).getChildren();
		    if (spanElems.size() < 1) {
			throw new IllegalStateException("Every coref must contain a 'span' element inside 'references'");
		    }
		    List<Span<Term>> references = new ArrayList<Span<Term>>();
		    for (Element spanElem : spanElems) {
			Span<Term> span = kaf.newTermSpan();
			List<Element> targetElems = spanElem.getChildren();
			if (targetElems.size() < 1) {
			    throw new IllegalStateException("Every span in an entity must contain at least one target inside");  
			}
			for (Element targetElem : targetElems) {
			    String targetTermId = getAttribute("id", targetElem);
			    Term targetTerm = termIndex.get(targetTermId);
			    if (targetTerm == null) {
				throw new KAFNotValidException("Term " + targetTermId + " not found when loading coref " + coId);
			    }
			    boolean isHead = isHead(targetElem);
			    span.addTarget(targetTerm, isHead);
			}
			references.add(span);
		    }
		    Coref newCoref = kaf.newCoref(coId, references);
		}
	    }
	    if (elem.getName().equals("features")) {
		Element propertiesElem = elem.getChild("properties");
		Element categoriesElem = elem.getChild("categories");
		if (propertiesElem != null) {
		    List<Element> propertyElems = propertiesElem.getChildren("property");
		    for (Element propertyElem : propertyElems) {
			String pid = getAttribute("pid", propertyElem);
			String lemma = getAttribute("lemma", propertyElem);
			Element referencesElem = propertyElem.getChild("references");
			if (referencesElem == null) {
			    throw new IllegalStateException("Every property must contain a 'references' element");
			}
			List<Element> spanElems = referencesElem.getChildren("span");
			if (spanElems.size() < 1) {
			    throw new IllegalStateException("Every property must contain a 'span' element inside 'references'");
			}
			List<Span<Term>> references = new ArrayList<Span<Term>>();
			for (Element spanElem : spanElems) {
			    Span<Term> span = kaf.newTermSpan();
			    List<Element> targetElems = spanElem.getChildren();
			    if (targetElems.size() < 1) {
				throw new IllegalStateException("Every span in a property must contain at least one target inside");  
			    }
			    for (Element targetElem : targetElems) {
				String targetTermId = getAttribute("id", targetElem);
				Term targetTerm = termIndex.get(targetTermId);
				if (targetTerm == null) {
				    throw new KAFNotValidException("Term " + targetTermId + " not found when loading property " + pid);
				}
				boolean isHead = isHead(targetElem);
				span.addTarget(targetTerm, isHead);
			    }
			    references.add(span);
			}
			Feature newProperty = kaf.newProperty(pid, lemma, references);
			List<Element> externalReferencesElems = propertyElem.getChildren("externalReferences");
			if (externalReferencesElems.size() > 0) {
			    List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			    newProperty.addExternalRefs(externalRefs);
			}
			relationalIndex.put(newProperty.getId(), newProperty);
		    }
		}
		if (categoriesElem != null) {
		    List<Element> categoryElems = categoriesElem.getChildren("category");
		    for (Element categoryElem : categoryElems) {
			String cid = getAttribute("cid", categoryElem);
			String lemma = getAttribute("lemma", categoryElem);
			Element referencesElem = categoryElem.getChild("references");
			if (referencesElem == null) {
			    throw new IllegalStateException("Every category must contain a 'references' element");
			}
			List<Element> spanElems = referencesElem.getChildren("span");
			if (spanElems.size() < 1) {
			    throw new IllegalStateException("Every category must contain a 'span' element inside 'references'");
			}
			List<Span<Term>> references = new ArrayList<Span<Term>>();
			for (Element spanElem : spanElems) {
			    Span<Term> span = kaf.newTermSpan();
			    List<Element> targetElems = spanElem.getChildren();
			    if (targetElems.size() < 1) {
				throw new IllegalStateException("Every span in a property must contain at least one target inside");  
			    }
			    for (Element targetElem : targetElems) {
				String targetTermId = getAttribute("id", targetElem);
				Term targetTerm = termIndex.get(targetTermId);
				if (targetTerm == null) {
				    throw new KAFNotValidException("Term " + targetTermId + " not found when loading category " + cid);
				}
				boolean isHead = isHead(targetElem);
				span.addTarget(targetTerm, isHead);
			    }
			    references.add(span);
			}
			Feature newCategory = kaf.newCategory(cid, lemma, references);
			List<Element> externalReferencesElems = categoryElem.getChildren("externalReferences");
			if (externalReferencesElems.size() > 0) {
			    List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			    newCategory.addExternalRefs(externalRefs);
			}
			relationalIndex.put(newCategory.getId(), newCategory);
		    }
		}
	    }
	    if (elem.getName().equals("opinions")) {
		List<Element> opinionElems = elem.getChildren("opinion");
		for (Element opinionElem : opinionElems) {
		    String opinionId = getAttribute("oid", opinionElem);
		    Opinion opinion = kaf.newOpinion(opinionId);
		    Element opinionHolderElem = opinionElem.getChild("opinion_holder");
		    if (opinionHolderElem != null) {
			Span<Term> span = kaf.newTermSpan();
			Opinion.OpinionHolder opinionHolder = opinion.createOpinionHolder(span);
			Element spanElem = opinionHolderElem.getChild("span");
			if (spanElem != null) {
			    List<Element> targetElems = spanElem.getChildren("target");
			    for (Element targetElem : targetElems) {
				String refId = getOptAttribute("id", targetElem);
				boolean isHead = isHead(targetElem);
				Term targetTerm = termIndex.get(refId);
				if (targetTerm == null) {
				    throw new KAFNotValidException("Term " + refId + " not found when loading opinion " + opinionId);
				}
				span.addTarget(targetTerm, isHead);
			    }
			}
		    }
		    Element opinionTargetElem = opinionElem.getChild("opinion_target");
		    if (opinionTargetElem != null) {
			Span<Term> span = kaf.newTermSpan();
			Opinion.OpinionTarget opinionTarget = opinion.createOpinionTarget(span);
			Element spanElem = opinionTargetElem.getChild("span");
			if (spanElem != null) {
			    List<Element> targetElems = spanElem.getChildren("target");
			    for (Element targetElem : targetElems) {
				String refId = getOptAttribute("id", targetElem);
				boolean isHead = isHead(targetElem);
				Term targetTerm = termIndex.get(refId);
				if (targetTerm == null) {
				    throw new KAFNotValidException("Term " + refId + " not found when loading opinion " + opinionId);
				}
				span.addTarget(targetTerm, isHead);
			    }
			}
		    }
		    Element opinionExpressionElem = opinionElem.getChild("opinion_expression");
		    if (opinionExpressionElem != null) {
			Span<Term> span = kaf.newTermSpan();
			String polarity = getOptAttribute("polarity", opinionExpressionElem);
			String strength = getOptAttribute("strength", opinionExpressionElem);
			String subjectivity = getOptAttribute("subjectivity", opinionExpressionElem);
			String sentimentSemanticType = getOptAttribute("sentiment_semantic_type", opinionExpressionElem);
			String sentimentProductFeature = getOptAttribute("sentiment_product_feature", opinionExpressionElem);
			Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(span);
			if (polarity != null) {
			    opinionExpression.setPolarity(polarity);
			}
			if (strength != null) {
			    opinionExpression.setStrength(strength);
			}
			if (subjectivity != null) {
			    opinionExpression.setSubjectivity(subjectivity);
			}
			if (sentimentSemanticType != null) {
			    opinionExpression.setSentimentSemanticType(sentimentSemanticType);
			}
			if (sentimentProductFeature != null) {
			    opinionExpression.setSentimentProductFeature(sentimentProductFeature);
			}

			Element spanElem = opinionExpressionElem.getChild("span");
			if (spanElem != null) {
			    List<Element> targetElems = spanElem.getChildren("target");
			    for (Element targetElem : targetElems) {
				String refId = getOptAttribute("id", targetElem);
				boolean isHead = isHead(targetElem);
				Term targetTerm = termIndex.get(refId);
				if (targetTerm == null) {
				    throw new KAFNotValidException("Term " + refId + " not found when loading opinion " + opinionId);
				}
				span.addTarget(targetTerm, isHead);
			    }
			}
		    }
		}
	    }
	    if (elem.getName().equals("relations")) {
		List<Element> relationElems = elem.getChildren("relation");
		for (Element relationElem : relationElems) {
		    String id = getAttribute("rid", relationElem);
		    String fromId = getAttribute("from", relationElem);
		    String toId = getAttribute("to", relationElem);
		    String confidenceStr = getOptAttribute("confidence", relationElem);
		    float confidence = -1.0f;
		    if (confidenceStr != null) {
			confidence = Float.parseFloat(confidenceStr);
		    }
		    Relational from = relationalIndex.get(fromId);
		    if (from == null) {
			throw new KAFNotValidException("Entity/feature object " + fromId + " not found when loading relation " + id);
		    }
		    Relational to = relationalIndex.get(toId);
		    if (to == null) {
			throw new KAFNotValidException("Entity/feature object " + toId + " not found when loading relation " + id);
		    }
		    Relation newRelation = kaf.newRelation(id, from, to);
		    if (confidence >= 0) {
			newRelation.setConfidence(confidence);
		    }
		}
	    }
	    if (elem.getName().equals("srl")) {
		List<Element> predicateElems = elem.getChildren("predicate");
		for (Element predicateElem : predicateElems) {
		    String id = getAttribute("prid", predicateElem);
		    String uri = getOptAttribute("uri", predicateElem);
		    Span<Term> span = kaf.newTermSpan();
		    Element spanElem = predicateElem.getChild("span");
		    if (spanElem != null) {
			List<Element> targetElems = spanElem.getChildren("target");
			for (Element targetElem : targetElems) {
			    String targetId = getAttribute("id", targetElem);
			    boolean isHead = isHead(targetElem);
			    Term targetTerm = termIndex.get(targetId);
			    if (targetTerm == null) {
				throw new KAFNotValidException("Term object " + targetId + " not found when loading predicate " + id);				
			    }
			    span.addTarget(targetTerm, isHead);
			}
		    }
		    Predicate newPredicate = kaf.newPredicate(id, span);
		    if (uri != null) {
			newPredicate.setUri(uri);
		    }
		    List<Element> roleElems = predicateElem.getChildren("role");
		    for (Element roleElem : roleElems) {
			String rid = getAttribute("rid", roleElem);
			String semRole = getAttribute("semRole", roleElem);
			Span<Term> roleSpan = kaf.newTermSpan();
			Element roleSpanElem = roleElem.getChild("span");
			if (roleSpanElem != null) {
			    List<Element> targetElems = roleSpanElem.getChildren("target");
			    for (Element targetElem : targetElems) {
				String targetId = getAttribute("id", targetElem);
				boolean isHead = isHead(targetElem);
				Term targetTerm = termIndex.get(targetId);
				if (targetTerm == null) {
				    throw new KAFNotValidException("Term object " + targetId + " not found when loading role " + rid);				
				}
				roleSpan.addTarget(targetTerm, isHead);
			    }
			}
			Predicate.Role newRole = kaf.newRole(rid, newPredicate, semRole, roleSpan);
			newPredicate.addRole(newRole);
		    }
		    Span<Term> spana = kaf.newTermSpan();
		    Predicate.Role rolea = kaf.newRole(newPredicate, "kaka", spana);
		    newPredicate.addRole(rolea);
		}
	    }
	    if (elem.getName().equals("constituents")) {
		List<Element> treeElems = elem.getChildren("tree");
		for (Element treeElem : treeElems) {
		    HashMap<String, TreeNode> treeNodes = new HashMap<String, TreeNode>();
		    HashMap<String, Boolean> rootNodes = new HashMap<String, Boolean>();
		    // Terminals
		    List<Element> terminalElems = treeElem.getChildren("t");
		    for (Element terminalElem : terminalElems) {
			String id = getAttribute("id", terminalElem);
			Element spanElem = terminalElem.getChild("span");
			if (spanElem == null) {
			    throw new KAFNotValidException("Constituent non terminal nodes need a span");
			}
			Span<Term> span = loadTermSpan(spanElem, termIndex, id);
			treeNodes.put(id, kaf.newTerminal(id, span));
			rootNodes.put(id, true);
		    }
		    // NonTerminals
		    List<Element> nonTerminalElems = treeElem.getChildren("nt");
		    for (Element nonTerminalElem : nonTerminalElems) {
			String id = getAttribute("id", nonTerminalElem);
			String label = getAttribute("label", nonTerminalElem);
			treeNodes.put(id, kaf.newNonTerminal(id, label));
			rootNodes.put(id, true);
		    }
		    // Edges
		    List<Element> edgeElems = treeElem.getChildren("edge");
		    for (Element edgeElem : edgeElems) {
			String fromId = getAttribute("from", edgeElem);
			String toId = getAttribute("to", edgeElem);
			TreeNode parentNode = treeNodes.get(toId);
			TreeNode childNode = treeNodes.get(fromId);
			if ((parentNode == null) || (childNode == null)) {
			    throw new KAFNotValidException("There is an problem with the edge(" + fromId + ", " + toId + "). One of its targets doesn't exist.");
			}
			((NonTerminal) parentNode).addChild(childNode);
			rootNodes.put(fromId, false);
		    }
		    // Constituent objects
		    for (Map.Entry<String, Boolean> areRoot : rootNodes.entrySet()) {
			if (areRoot.getValue()) {
			    TreeNode rootNode = treeNodes.get(areRoot.getKey());
			    kaf.newConstituent(rootNode);
			}
		    }
		}
	    }
	}

	return kaf;
    }

    private static Span<Term> loadTermSpan(Element spanElem, HashMap<String, Term> terms, String objId) throws KAFNotValidException {
	List<Element> targetElems = spanElem.getChildren("target");
	if (targetElems.size() < 1) {
	    throw new KAFNotValidException("A span element can not be empty");
	}
	Span<Term> span = KAFDocument.newTermSpan();
	for (Element targetElem : targetElems) {
	    String targetId = getAttribute("id", targetElem);
	    boolean isHead = isHead(targetElem);
	    Term targetTerm = terms.get(targetId);
	    if (targetTerm == null) {
		throw new KAFNotValidException("Term object " + targetId + " not found when loading object " + objId);
	    }
	    span.addTarget(targetTerm, isHead);
	}
	return span;
    }

    private static Element createTermSpanElem(Span<Term> span) {
	Element spanElem = new Element("span");
	for (Term term : span.getTargets()) {
	    Element targetElem = new Element("target");
	    String targetId = term.getId();
	    targetElem.setAttribute("id", targetId);
	    if (span.isHead(term)) {
		targetElem.setAttribute("head", "yes");
	    }
	    spanElem.addContent(targetElem);
	}
	return spanElem;
    }

    private static List<ExternalRef> getExternalReferences(Element externalReferencesElem, KAFDocument kaf) {
	List<ExternalRef> externalRefs = new ArrayList<ExternalRef>();
	List<Element> externalRefElems = externalReferencesElem.getChildren();
	for (Element externalRefElem : externalRefElems) {
	    ExternalRef externalRef = getExternalRef(externalRefElem, kaf);
	    externalRefs.add(externalRef);
	}
	return externalRefs;
    }

    private static ExternalRef getExternalRef(Element externalRefElem, KAFDocument kaf) {
	String resource = getAttribute("resource", externalRefElem);
	String references = getAttribute("reference", externalRefElem);
	ExternalRef newExternalRef = kaf.newExternalRef(resource, references);
	String confidence = getOptAttribute("confidence", externalRefElem);
	if (confidence != null) {
	    newExternalRef.setConfidence(Float.valueOf(confidence));
	}
	List<Element> subRefElems = externalRefElem.getChildren("externalRef");
	if (subRefElems.size() > 0) {
	    Element subRefElem = subRefElems.get(0);
	    ExternalRef subRef = getExternalRef(subRefElem, kaf);
	    newExternalRef.setExternalRef(subRef);
	}
	return newExternalRef;
    }

    private static String getAttribute(String attName, Element elem) {
	String value = elem.getAttributeValue(attName);
	if (value==null) {
	    throw new IllegalStateException(attName+" attribute must be defined for element "+elem.getName());
	}
	return value;
    }

    private static String getAttribute(String attName, Element elem, Namespace nmspace) {
	String value = elem.getAttributeValue(attName, nmspace);
	if (value==null) {
	    throw new IllegalStateException(attName+" attribute must be defined for element "+elem.getName());
	}
	return value;
    }

    private static String getOptAttribute(String attName, Element elem) {
	String value = elem.getAttributeValue(attName);
	if (value==null) {
	    return null;
	}
	return value;
    }

    private static boolean isHead(Element elem) {
	String value = elem.getAttributeValue("head");
	if (value == null) {
	    return false;
	}
	if (value.equals("yes")) {
	    return true;
	}
	return false;
    }


    private static class Edge {
	String from;
	String to;

	Edge(String from, String to) {
	    this.from = from;
	    this.to = to;
	}
    }

    /** Returns the content of the given KAFDocument in a DOM document. */
    private static Document KAFToDOM(KAFDocument kaf) {
	AnnotationContainer annotationContainer = kaf.getAnnotationContainer();
	Element root = new Element("NAF");
	root.setAttribute("lang", kaf.getLang(), Namespace.XML_NAMESPACE);
	root.setAttribute("version", kaf.getVersion());

	Document doc = new Document(root);

	Element kafHeaderElem = new Element("nafHeader");
	root.addContent(kafHeaderElem);

	KAFDocument.FileDesc fd = kaf.getFileDesc();
	if (fd != null) {
	    Element fdElem = new Element("fileDesc");
	    if (fd.author != null) {
		fdElem.setAttribute("author", fd.author);
	    }
	    if (fd.author != null) {
		fdElem.setAttribute("title", fd.title);
	    }
	    if (fd.creationtime != null) {
		fdElem.setAttribute("creationtime", fd.creationtime);
	    }
	    if (fd.author != null) {
		fdElem.setAttribute("filename", fd.filename);
	    }
	    if (fd.author != null) {
		fdElem.setAttribute("filetype", fd.filetype);
	    }
	    if (fd.author != null) {
		fdElem.setAttribute("pages", Integer.toString(fd.pages));
	    }
	    kafHeaderElem.addContent(fdElem);
	}

	KAFDocument.Public pub = kaf.getPublic();
	if (pub != null) {
	    Element pubElem = new Element("public");
	    pubElem.setAttribute("publicId", pub.publicId);
	    if (pub.uri != null) {
		pubElem.setAttribute("uri", pub.uri);
	    }
	    kafHeaderElem.addContent(pubElem);
	}

	HashMap<String, List<KAFDocument.LinguisticProcessor>> lps = kaf.getLinguisticProcessors();
	for (Map.Entry entry : lps.entrySet()) {
	    Element lpsElem = new Element("linguisticProcessors");
	    lpsElem.setAttribute("layer", (String) entry.getKey());
	    for (KAFDocument.LinguisticProcessor lp : (List<KAFDocument.LinguisticProcessor>) entry.getValue()) {
		Element lpElem = new Element("lp");
		lpElem.setAttribute("name", lp.name);
		lpElem.setAttribute("timestamp", lp.timestamp);
		lpElem.setAttribute("version", lp.version);
		lpsElem.addContent(lpElem);
	    }
	    kafHeaderElem.addContent(lpsElem);
	}

	List<WF> text = annotationContainer.getText();
	if (text.size() > 0) {
	    Element textElem = new Element("text");
	    for (WF wf : text) {
		Element wfElem = new Element("wf");
		wfElem.setAttribute("wid", wf.getId());
		if (wf.hasSent()) {
		    wfElem.setAttribute("sent", Integer.toString(wf.getSent()));
		}
		if (wf.hasPara()) {
		    wfElem.setAttribute("para", Integer.toString(wf.getPara()));
		}
		if (wf.hasPage()) {
		    wfElem.setAttribute("page", Integer.toString(wf.getPage()));
		}
		if (wf.hasOffset()) {
		    wfElem.setAttribute("offset", Integer.toString(wf.getOffset()));
		}
		if (wf.hasLength()) {
		    wfElem.setAttribute("length", Integer.toString(wf.getLength()));
		}
		if (wf.hasXpath()) {
		    wfElem.setAttribute("xpath", wf.getXpath());
		}
		wfElem.setText(wf.getForm());
		textElem.addContent(wfElem);
	    }
	    root.addContent(textElem);
	}

	List<Term> terms = annotationContainer.getTerms();
	if (terms.size() > 0) {
	    Element termsElem = new Element("terms");
	    for (Term term : terms) {
		String morphofeat;
		Term.Component head;
		String termcase;
		Comment termComment = new Comment(term.getStr());
		termsElem.addContent(termComment);
		Element termElem = new Element("term");
		termElem.setAttribute("tid", term.getId());
		termElem.setAttribute("type", term.getType());
		termElem.setAttribute("lemma", term.getLemma());
		termElem.setAttribute("pos", term.getPos());
		if (term.hasMorphofeat()) {
		    termElem.setAttribute("morphofeat", term.getMorphofeat());
		}
		if (term.hasHead()) {
		    termElem.setAttribute("head", term.getHead().getId());
		}
		if (term.hasCase()) {
		    termElem.setAttribute("case", term.getCase());
		}
		if (term.hasSentiment()) {
		    Term.Sentiment sentiment = term.getSentiment();
		    Element sentimentElem = new Element("sentiment");
		    if (sentiment.hasResource()) {
			sentimentElem.setAttribute("resource", sentiment.getResource());
		    }
		    if (sentiment.hasPolarity()) {
			sentimentElem.setAttribute("polarity", sentiment.getPolarity());
		    }
		    if (sentiment.hasStrength()) {
			sentimentElem.setAttribute("strength", sentiment.getStrength());
		    }
		    if (sentiment.hasSubjectivity()) {
			sentimentElem.setAttribute("subjectivity", sentiment.getSubjectivity());
		    }
		    if (sentiment.hasSentimentSemanticType()) {
			sentimentElem.setAttribute("sentiment_semantic_type", sentiment.getSentimentSemanticType());
		    }
		    if (sentiment.hasSentimentModifier()) {
			sentimentElem.setAttribute("sentiment_modifier", sentiment.getSentimentModifier());
		    }
		    if (sentiment.hasSentimentMarker()) {
			sentimentElem.setAttribute("sentiment_marker", sentiment.getSentimentMarker());
		    }
		    if (sentiment.hasSentimentProductFeature()) {
			sentimentElem.setAttribute("sentiment_product_feature", sentiment.getSentimentProductFeature());
		    }
		    termElem.addContent(sentimentElem);
		}
		Element spanElem = new Element("span");
		Span<WF> span = term.getSpan();
		for (WF target : term.getWFs()) {
		    Element targetElem = new Element("target");
		    targetElem.setAttribute("id", target.getId());
		    if (target == span.getHead()) {
			targetElem.setAttribute("head", "yes");
		    }
		    spanElem.addContent(targetElem);
		}
		termElem.addContent(spanElem);
		List<Term.Component> components = term.getComponents();
		if (components.size() > 0) {
		    for (Term.Component component : components) {
			Element componentElem = new Element("component");
			componentElem.setAttribute("id", component.getId());
			componentElem.setAttribute("lemma", component.getLemma());
			componentElem.setAttribute("pos", component.getPos());
			if (component.hasCase()) {
			    componentElem.setAttribute("case", component.getCase());
			}
			List<ExternalRef> externalReferences = component.getExternalRefs();
			if (externalReferences.size() > 0) {
			    Element externalReferencesElem = externalReferencesToDOM(externalReferences);
			    componentElem.addContent(externalReferencesElem);
			}
			termElem.addContent(componentElem);
		    }
		}
		List<ExternalRef> externalReferences = term.getExternalRefs();
		if (externalReferences.size() > 0) {
		    Element externalReferencesElem = externalReferencesToDOM(externalReferences);
		    termElem.addContent(externalReferencesElem);
		}
		termsElem.addContent(termElem);
	    }
	    root.addContent(termsElem);
	}

	List<Dep> deps = annotationContainer.getDeps();
	if (deps.size() > 0) {
	    Element depsElem = new Element("deps");
	    for (Dep dep : deps) {
		Comment depComment = new Comment(dep.getStr());
		depsElem.addContent(depComment);
		Element depElem = new Element("dep");
		depElem.setAttribute("from", dep.getFrom().getId());
		depElem.setAttribute("to", dep.getTo().getId());
		depElem.setAttribute("rfunc", dep.getRfunc());
		if (dep.hasCase()) {
		    depElem.setAttribute("case", dep.getCase());
		}
		depsElem.addContent(depElem);
	    }
	    root.addContent(depsElem);
	}

	List<Chunk> chunks = annotationContainer.getChunks();
	if (chunks.size() > 0) {
	    Element chunksElem = new Element("chunks");
	    for (Chunk chunk : chunks) {
		Comment chunkComment = new Comment(chunk.getStr());
		chunksElem.addContent(chunkComment);
		Element chunkElem = new Element("chunk");
		chunkElem.setAttribute("cid", chunk.getId());
		chunkElem.setAttribute("head", chunk.getHead().getId());
		chunkElem.setAttribute("phrase", chunk.getPhrase());
		if (chunk.hasCase()) {
		    chunkElem.setAttribute("case", chunk.getCase());
		}
		Element spanElem = new Element("span");
		for (Term target : chunk.getTerms()) {
		    Element targetElem = new Element("target");
		    targetElem.setAttribute("id", target.getId());
		    spanElem.addContent(targetElem);
		}
		chunkElem.addContent(spanElem);
		chunksElem.addContent(chunkElem);
	    }
	    root.addContent(chunksElem);
	}

	List<Entity> entities = annotationContainer.getEntities();
	if (entities.size() > 0) {
	    Element entitiesElem = new Element("entities");
	    for (Entity entity : entities) {
		Element entityElem = new Element("entity");
		entityElem.setAttribute("eid", entity.getId());
		entityElem.setAttribute("type", entity.getType());
		Element referencesElem = new Element("references");
		for (Span<Term> span : entity.getSpans()) {
		    Comment spanComment = new Comment(entity.getSpanStr(span));
		    referencesElem.addContent(spanComment);
		    Element spanElem = new Element("span");
		    for (Term term : span.getTargets()) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", term.getId());
			if (term == span.getHead()) {
			    targetElem.setAttribute("head", "yes");
			}
			spanElem.addContent(targetElem);
		    }
		    referencesElem.addContent(spanElem);
		}
		entityElem.addContent(referencesElem);
		List<ExternalRef> externalReferences = entity.getExternalRefs();
		if (externalReferences.size() > 0) {
		    Element externalReferencesElem = externalReferencesToDOM(externalReferences);
		    entityElem.addContent(externalReferencesElem);
		}
		entitiesElem.addContent(entityElem);
	    }
	    root.addContent(entitiesElem);
	}

	List<Coref> corefs = annotationContainer.getCorefs();
	if (corefs.size() > 0) {
	    Element corefsElem = new Element("coreferences");
	    for (Coref coref : corefs) {
		Element corefElem = new Element("coref");
		corefElem.setAttribute("coid", coref.getId());
		Element referencesElem = new Element("references");
		for (Span<Term> span : coref.getSpans()) {
		    Comment spanComment = new Comment(coref.getSpanStr(span));
		    referencesElem.addContent(spanComment);
		    Element spanElem = new Element("span");
		    for (Term target : span.getTargets()) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", target.getId());
			if (target == span.getHead()) {
			    targetElem.setAttribute("head", "yes");
			}
			spanElem.addContent(targetElem);
		    }
		    referencesElem.addContent(spanElem);
		}
		corefElem.addContent(referencesElem);
		corefsElem.addContent(corefElem);
	    }
	    root.addContent(corefsElem);
	}

	Element featuresElem = new Element("features");
	List<Feature> properties = annotationContainer.getProperties();
	if (properties.size() > 0) {
	    Element propertiesElem = new Element("properties");
	    for (Feature property : properties) {
		Element propertyElem = new Element("property");
		propertyElem.setAttribute("pid", property.getId());
		propertyElem.setAttribute("lemma", property.getLemma());
		List<Span<Term>> references = property.getSpans();
		Element referencesElem = new Element("references");
		for (Span<Term> span : references) {
		    Comment spanComment = new Comment(property.getSpanStr(span));
		    referencesElem.addContent(spanComment);
		    Element spanElem = new Element("span");
		    for (Term term : span.getTargets()) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", term.getId());
			if (term == span.getHead()) {
			    targetElem.setAttribute("head", "yes");
			}
			spanElem.addContent(targetElem);
		    }
		    referencesElem.addContent(spanElem);
		}
		propertyElem.addContent(referencesElem);
		propertiesElem.addContent(propertyElem);
	    }
	    featuresElem.addContent(propertiesElem);
	}
	List<Feature> categories = annotationContainer.getCategories();
	if (categories.size() > 0) {
	    Element categoriesElem = new Element("categories");
	    for (Feature category : categories) {
		Element categoryElem = new Element("category");
		categoryElem.setAttribute("cid", category.getId());
		categoryElem.setAttribute("lemma", category.getLemma());
		List<Span<Term>> references = category.getSpans();
		Element referencesElem = new Element("references");
		for (Span<Term> span : references) {
		    Comment spanComment = new Comment(category.getSpanStr(span));
		    referencesElem.addContent(spanComment);
		    Element spanElem = new Element("span");
		    for (Term term : span.getTargets()) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", term.getId());
			if (term == span.getHead()) {
			    targetElem.setAttribute("head", "yes");
			}
			spanElem.addContent(targetElem);
		    }
		    referencesElem.addContent(spanElem);
		}
		categoryElem.addContent(referencesElem);
		categoriesElem.addContent(categoryElem);
	    }
	    featuresElem.addContent(categoriesElem);
	}
	if (featuresElem.getChildren().size() > 0) {
	    root.addContent(featuresElem);
	}

	List<Opinion> opinions = annotationContainer.getOpinions();
	if (opinions.size() > 0) {
	    Element opinionsElem = new Element("opinions");
	    for (Opinion opinion : opinions) {
		Element opinionElem = new Element("opinion");
		opinionElem.setAttribute("oid", opinion.getId());
		Opinion.OpinionHolder holder = opinion.getOpinionHolder();
		if (holder != null) {
		    Element opinionHolderElem = new Element("opinion_holder");
		    Comment comment = new Comment(opinion.getSpanStr(opinion.getOpinionHolder().getSpan()));
		    opinionHolderElem.addContent(comment);
		    List<Term> targets = holder.getTerms();
		    Span<Term> span = holder.getSpan();
		    if (targets.size() > 0) {
			Element spanElem = new Element("span");
			opinionHolderElem.addContent(spanElem);
			for (Term target : targets) {
			    Element targetElem = new Element("target");
			    targetElem.setAttribute("id", target.getId());
			    if (target == span.getHead()) {
				targetElem.setAttribute("head", "yes");
			    }
			    spanElem.addContent(targetElem);
			}
		    }
		    opinionElem.addContent(opinionHolderElem);
		}
		Opinion.OpinionTarget opTarget = opinion.getOpinionTarget();
		if (opTarget != null) {
		    Element opinionTargetElem = new Element("opinion_target");
		    Comment comment = new Comment(opinion.getSpanStr(opinion.getOpinionTarget().getSpan()));
		    opinionTargetElem.addContent(comment);
		    List<Term> targets = opTarget.getTerms();
		    Span<Term> span = opTarget.getSpan();
		    if (targets.size() > 0) {
			Element spanElem = new Element("span");
			opinionTargetElem.addContent(spanElem);
			for (Term target : targets) {
			    Element targetElem = new Element("target");
			    targetElem.setAttribute("id", target.getId());
			    if (target == span.getHead()) {
				targetElem.setAttribute("head", "yes");
			    }
			    spanElem.addContent(targetElem);
			}
		    }
		    opinionElem.addContent(opinionTargetElem);
		}
		Opinion.OpinionExpression expression = opinion.getOpinionExpression();
		if (expression != null) {
		    Element opinionExpressionElem = new Element("opinion_expression");
		    Comment comment = new Comment(opinion.getSpanStr(opinion.getOpinionExpression().getSpan()));
		    opinionExpressionElem.addContent(comment);
		    if (expression.hasPolarity()) {
			opinionExpressionElem.setAttribute("polarity", expression.getPolarity());
		    }
		    if (expression.hasStrength()) {
			opinionExpressionElem.setAttribute("strength", expression.getStrength());
		    }
		    if (expression.hasSubjectivity()) {
			opinionExpressionElem.setAttribute("subjectivity", expression.getSubjectivity());
		    }
		    if (expression.hasSentimentSemanticType()) {
		    opinionExpressionElem.setAttribute("sentiment_semantic_type", expression.getSentimentSemanticType());
		    }
		    if (expression.hasSentimentProductFeature()) {
			opinionExpressionElem.setAttribute("sentiment_product_feature", expression.getSentimentProductFeature());
		    }
		    List<Term> targets = expression.getTerms();
		    Span<Term> span = expression.getSpan();
		    if (targets.size() > 0) {
			Element spanElem = new Element("span");
			opinionExpressionElem.addContent(spanElem);
			for (Term target : targets) {
			    Element targetElem = new Element("target");
			    targetElem.setAttribute("id", target.getId());
			    if (target == span.getHead()) {
				targetElem.setAttribute("head", "yes");
			    }
			    spanElem.addContent(targetElem);
			}
		    }
		    opinionElem.addContent(opinionExpressionElem);
		}

		opinionsElem.addContent(opinionElem);
	    }
	    root.addContent(opinionsElem);
	}

	List<Relation> relations = annotationContainer.getRelations();
	if (relations.size() > 0) {
	    Element relationsElem = new Element("relations");
	    for (Relation relation : relations) {
		Comment comment = new Comment(relation.getStr());
		relationsElem.addContent(comment);
		Element relationElem = new Element("relation");
		relationElem.setAttribute("rid", relation.getId());
		relationElem.setAttribute("from", relation.getFrom().getId());
		relationElem.setAttribute("to", relation.getTo().getId());
		if (relation.hasConfidence()) {
		    relationElem.setAttribute("confidence", String.valueOf(relation.getConfidence()));
		}
		relationsElem.addContent(relationElem);
	    }
	    root.addContent(relationsElem);
	}

	List<Predicate> predicates = annotationContainer.getPredicates();
	if (predicates.size() > 0) {
	    Element predicatesElem = new Element("srl");
	    for (Predicate predicate : predicates) {
		Comment predicateComment = new Comment(predicate.getStr());
		predicatesElem.addContent(predicateComment);
		Element predicateElem = new Element("predicate");
		predicateElem.setAttribute("prid", predicate.getId());
		if (predicate.hasUri()) {
		    predicateElem.setAttribute("uri", predicate.getUri());
		}
		Span<Term> span = predicate.getSpan();
		if (span.getTargets().size() > 0) {
		    Comment spanComment = new Comment(predicate.getSpanStr());
		    Element spanElem = new Element("span");
		    predicateElem.addContent(spanComment);
		    predicateElem.addContent(spanElem);
		    for (Term target : span.getTargets()) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", target.getId());
			if (target == span.getHead()) {
			    targetElem.setAttribute("head", "yes");
			}
			spanElem.addContent(targetElem);
		    }
		}
		for (Predicate.Role role : predicate.getRoles()) {
		    Element roleElem = new Element("role");
		    roleElem.setAttribute("rid", role.getId());
		    roleElem.setAttribute("semRole", role.getSemRole());
		    Span<Term> roleSpan = role.getSpan();
		    if (roleSpan.getTargets().size() > 0) {
			Comment spanComment = new Comment(role.getStr());
			Element spanElem = new Element("span");
			roleElem.addContent(spanComment);
			roleElem.addContent(spanElem);
			for (Term target : roleSpan.getTargets()) {
			    Element targetElem = new Element("target");
			    targetElem.setAttribute("id", target.getId());
			    if (target == roleSpan.getHead()) {
				targetElem.setAttribute("head", "yes");
			    }
			    spanElem.addContent(targetElem);
			}
		    }
		    predicateElem.addContent(roleElem);
		}
		predicatesElem.addContent(predicateElem);
	    }
	    root.addContent(predicatesElem);
	}

	List<Tree> constituents = annotationContainer.getConstituents();
	if (constituents.size() > 0) {
	    Element constituentsElem = new Element("constituents");
	    for (Tree tree : constituents) {
		Element treeElem = new Element("tree");
		constituentsElem.addContent(treeElem);
		List<NonTerminal> nonTerminals = new LinkedList<NonTerminal>();
		List<Terminal> terminals = new LinkedList<Terminal>();
		List<Edge> edges = new ArrayList<Edge>();
		TreeNode rootNode = tree.getRoot();
		extractTreeNodes(rootNode, nonTerminals, terminals, edges);
		Collections.sort(nonTerminals, new Comparator<NonTerminal>() {
			public int compare(NonTerminal nt1, NonTerminal nt2) {
			    if (cmpId(nt1.getId(), nt2.getId()) < 0) {
				return -1;
			    } else if (nt1.getId().equals(nt2.getId())) {
				return 0;
			    } else {
				return 1;
			    }
			}
		    });
		Collections.sort(terminals, new Comparator<Terminal>() {
			public int compare(Terminal t1, Terminal t2) {
			    if (cmpId(t1.getId(), t2.getId()) < 0) {
				return -1;
			    } else if (t1.getId().equals(t2.getId())) {
				return 0;
			    } else {
				return 1;
			    }
			}
		    });
		for (NonTerminal node : nonTerminals) {
		    Element nodeElem = new Element("nt");
		    nodeElem.setAttribute("id", node.getId());
		    nodeElem.setAttribute("label", node.getLabel());
		    treeElem.addContent(nodeElem);
		}
		for (Terminal node : terminals) {
		    Element nodeElem = new Element("t");
		    nodeElem.setAttribute("id", node.getId());
		    nodeElem.addContent(createTermSpanElem(node.getSpan()));
		    treeElem.addContent(nodeElem);
		}
		for (Edge edge : edges) {
		    Element edgeElem = new Element("edge");
		    edgeElem.setAttribute("from", edge.from);
		    edgeElem.setAttribute("to", edge.to);
		    treeElem.addContent(edgeElem);
		}
	    }
	    root.addContent(constituentsElem);
	}
	
	return doc;
    }

    private static void extractTreeNodes(TreeNode node, List<NonTerminal> nonTerminals, List<Terminal> terminals, List<Edge> edges) {
	if (node instanceof NonTerminal) {
	    nonTerminals.add((NonTerminal) node);
	    List<TreeNode> treeNodes = ((NonTerminal) node).getChildren();
	    for (TreeNode child : treeNodes) {
		edges.add(new Edge(child.getId(), node.getId()));
		extractTreeNodes(child, nonTerminals, terminals, edges);
	    }
	} else {
	    terminals.add((Terminal) node);
	}
    }

    private static Element externalReferencesToDOM(List<ExternalRef> externalRefs) {
	Element externalReferencesElem = new Element("externalReferences");
	for (ExternalRef externalRef : externalRefs) {
	    Element externalRefElem = externalRefToDOM(externalRef);
	    externalReferencesElem.addContent(externalRefElem);
	}
	return externalReferencesElem;
    }

    private static Element externalRefToDOM(ExternalRef externalRef) {
	Element externalRefElem = new Element("externalRef");
	externalRefElem.setAttribute("resource", externalRef.getResource());
	externalRefElem.setAttribute("reference", externalRef.getReference());
	if (externalRef.hasConfidence()) {
	    externalRefElem.setAttribute("confidence", Float.toString(externalRef.getConfidence()));
	}
	if (externalRef.hasExternalRef()) {
	    Element subExternalRefElem = externalRefToDOM(externalRef.getExternalRef());
	    externalRefElem.addContent(subExternalRefElem);
	}
	return externalRefElem;
    }

    private static int cmpId(String id1, String id2) {
	int nbr1 = extractNumberFromId(id1);
	int nbr2 = extractNumberFromId(id2);
	if (nbr1 < nbr2) {
	    return -1;
	} else if (nbr1 == nbr2) {
	    return 0;
	} else {
	    return 1;
	}
    }

    private static int extractNumberFromId(String id) {
	Matcher matcher = Pattern.compile("^[a-z]*_?(\\d+)$").matcher(id);
	if (!matcher.find()) {
	    throw new IllegalStateException("IdManager doesn't recognise the given id's (" + id  + ") format. Should be [a-z]*_?[0-9]+");
	}
	return Integer.valueOf(matcher.group(1));
    }
}
