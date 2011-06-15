package org.zanata.adapter.xliff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.zanata.rest.dto.extensions.comment.SimpleComment;
import org.zanata.rest.dto.extensions.gettext.TextFlowExtension;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TextFlow;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

public class XliffWriter extends XliffCommon
{
   private void mkdirs(File dir) throws IOException
   {
      if (!dir.exists())
      {
         if (!dir.mkdirs())
            throw new IOException("unable to create output directory: " + dir);
      }
   }

   // Write document header with XML, xliff, file and body tag
   private void writeHeader(IndentingXMLStreamWriter writer, Resource doc) throws XMLStreamException
   {
      // XML tag
      writer.writeStartDocument("utf-8", "1.0");
      writer.writeComment("XLIFF document generated by Zanata. Visit Zanata.org for more infomation.");
      writer.writeCharacters("\n");
      // XLiff tag
      writer.writeStartElement("xliff");
      writer.writeNamespace("", "urn:oasis:names:tc:xliff:document:1.1");
      writer.writeNamespace("xyz", "urn:appInfo:Items");
      writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      writer.writeAttribute("xsi:schemaLocation", "urn:oasis:names:tc:xliff:document:1.1 http://www.oasis-open.org/committees/xliff/documents/xliff-core-1.1.xsd");
      writer.writeAttribute("version", "1.1");

      // file tag
      writer.writeStartElement(ELE_FILE);
      writer.writeAttribute(ATTRI_SOURCE_LANGUAGE, doc.getLang().getId());
      writer.writeAttribute(ATTRI_DATATYPE, "plaintext");

      // body tag
      writer.writeStartElement(ELE_BODY);
   }

   private void writeTransUnit(IndentingXMLStreamWriter writer, Resource doc) throws XMLStreamException
   {
      for (TextFlow textFlow : doc.getTextFlows())
      {
         writer.writeStartElement(ELE_TRANS_UNIT);
         writer.writeAttribute(ATTRI_ID, textFlow.getId());
         writeTransUnitSource(writer, textFlow);
         writeTransUnitContext(writer, textFlow);
         writer.writeEndElement();// end trans-unit tag
      }
   }

   private void writeTransUnitSource(IndentingXMLStreamWriter writer, TextFlow textFlow) throws XMLStreamException
   {
      writer.writeStartElement(ELE_SOURCE);
      writer.writeCharacters(textFlow.getContent());
      writer.writeEndElement();// end source tag
   }

   private void writeTransUnitContext(IndentingXMLStreamWriter writer, TextFlow textFlow) throws XMLStreamException
   {
      if (!textFlow.getExtensions(true).isEmpty())
      {
         Map<String, ArrayList<String[]>> contextGroupMap = new HashMap<String, ArrayList<String[]>>();

         for (TextFlowExtension textFlowExtension : textFlow.getExtensions())
         {
            SimpleComment comment = (SimpleComment) textFlowExtension;
            String[] contextValues = comment.getValue().split(DELIMITER);
            if (!contextGroupMap.containsKey(contextValues[0]))
            {
               ArrayList<String[]> list = new ArrayList<String[]>();
               list.add(new String[] { contextValues[1], contextValues[2] });
               contextGroupMap.put(contextValues[0], list);
            }
            else
            {
               ArrayList<String[]> list = contextGroupMap.get(contextValues[0]);
               list.add(new String[] { contextValues[1], contextValues[2] });
            }

         }

         for (String key : contextGroupMap.keySet())
         {
            ArrayList<String[]> values = contextGroupMap.get(key);

            writer.writeStartElement(ELE_CONTEXT_GROUP);
            writer.writeAttribute(ATTRI_NAME, key);

            for (String[] val : values)
            {
               writer.writeStartElement(ELE_CONTEXT);
               writer.writeAttribute(ATTRI_CONTEXT_TYPE, val[0]);
               writer.writeCharacters(val[1]);
               writer.writeEndElement();// end context
            }

            writer.writeEndElement();// end context-group
         }
      }
   }

   public void write(File baseDir, Resource doc)
   {
      try
      {
         XMLOutputFactory output = XMLOutputFactory.newInstance();
         mkdirs(baseDir);
         XMLStreamWriter xmlStreamWriter = output.createXMLStreamWriter(new FileWriter(baseDir + "/" + doc.getName()));
         IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(xmlStreamWriter);

         writeHeader(writer, doc);
         writeTransUnit(writer, doc);

         writer.writeEndElement(); // end body tag
         writer.writeEndElement(); // end file tag
         writer.writeEndDocument(); // end Xliff tag
         writer.flush();
         writer.close();
      }
      catch (XMLStreamException e)
      {
         throw new RuntimeException("Error generating Xliff file format   ", e);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error writing Xliff file  ", e);
      }
   }

}