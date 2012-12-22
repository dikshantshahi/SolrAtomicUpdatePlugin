SolrAtomicUpdatePlugin
======================

Solr 4.0 Plugin which allows atomic update of lucene document more intuitively. The plugin supports update on the basis of Solr query. Update of multiValued field is also supported.

Internally Lucene doesn't allows you to update a document. The concept is simple delete and add.

This plugin allows you to update a document. It internally does a query, fetch, delete and add, thus releaving the client application from doing the same.
Another important feature of the plugin is that it allows you to perform SQL like update. It you are DB user, you will find this plugin more intuitive.
The user can specify solr query which uniquely identifies a document to be updated.



Installation Steps:
-------------------
1. Download the plugin file from dist/solr-atomic-update-plugin1.0.jar and place it in the SOLR_HOME/lib directory.
2. Add the following handler in solrconfig.xml
    <requestHandler name="/sql-like-update" class="com.idivine.solr.handler.SqlLikeUpdateRequestHandler">
     <lst name="defaults">
       <str name="update.chain">sql-like-processor</str>
     </lst>
    </requestHandler>
3. Add the following processor chain in solrconfig.xml
   <updateRequestProcessorChain name="sql-like-processor">
      <processor class="com.idivine.solr.processor.SqlLikeUpdateProcessorFactory" />
      <processor class="solr.LogUpdateProcessorFactory" />
      <processor class="solr.RunUpdateProcessorFactory" />
    </updateRequestProcessorChain>
4. Ensure that both the tags are added inside config tag.


Usage:
-----
Fields in document can be updated querying Solr just the way we query database. Here are different provisions to do that.

XML format to update a document:
<update>
  <doc>
		<query>standard solr query</query>
		<field name="field1">value1</field>
  	<field name="field2">value2</field>
    ...
	</doc>
  <doc>
    ...
  </doc>
</update>

Also, you can update the document as follows:
<update>
  <doc query="standard solr query">
		<field name="field1">value1</field>
		<field name="field2">value2</field>
    ...
	</doc>
  <doc>
    ...
  </doc>
</update>

If you know the uniqueId for the document, you can update document as follows:
<update>
  <doc>
    <query uniqueKey="true">value</query>
  	<field name="field1">value1</field>
  	<field name="field2">value2</field>
    ...
	</doc>
  <doc>
    ...
  </doc>
</update>

Example:
-------
Update the employee skills as follows:
$ curl http://localhost:8983/solr/sql-like-update -H 'Content-type:application/xml' -d '
<update>
  <doc>
  	<query>empId:12345</query>
		<field name="designation">SSE</field>
  	<field name="skills">Solr</field>
    <field name="skills">Lucene</field>
	</doc>
  <doc>
    ...
  </doc>
</update>'

Limitations:
-----------
1. All the fields should be stored.
2. Not tested for documents with index time boosts. Behavior unknown.
3. Each query should return only one document.
4. Has not been designed to support inc and adding values to a field, as Solr out of the box supports it. Please refer to Solr Wiki or by blog on out of the box feature http://dsahi.wordpress.com/2012/12/18/solr-atomic-update/.
