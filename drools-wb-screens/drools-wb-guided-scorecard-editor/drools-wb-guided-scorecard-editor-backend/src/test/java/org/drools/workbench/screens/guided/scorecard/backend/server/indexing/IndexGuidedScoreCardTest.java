/*
 * Copyright 2014 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.scorecard.backend.server.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.drools.workbench.models.datamodel.imports.Import;
import org.drools.workbench.models.guided.scorecard.backend.GuidedScoreCardXMLPersistence;
import org.drools.workbench.models.guided.scorecard.shared.ScoreCardModel;
import org.drools.workbench.screens.guided.scorecard.type.GuidedScoreCardResourceTypeDefinition;
import org.junit.Test;
import org.kie.workbench.common.services.refactoring.backend.server.BaseIndexingTest;
import org.kie.workbench.common.services.refactoring.backend.server.TestIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.indexing.RuleAttributeNameAnalyzer;
import org.kie.workbench.common.services.refactoring.backend.server.query.QueryBuilder;
import org.kie.workbench.common.services.refactoring.model.index.terms.RuleAttributeIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueFieldIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueTypeIndexTerm;
import org.uberfire.ext.metadata.backend.lucene.index.LuceneIndex;
import org.uberfire.ext.metadata.backend.lucene.util.KObjectUtil;
import org.uberfire.ext.metadata.engine.Index;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.java.nio.file.Path;

import static org.apache.lucene.util.Version.*;
import static org.junit.Assert.*;

public class IndexGuidedScoreCardTest extends BaseIndexingTest<GuidedScoreCardResourceTypeDefinition> {

    @Test
    public void testIndexGuidedScoreCard() throws IOException, InterruptedException {
        //Add test files
        final Path path1 = basePath.resolve( "scorecard1.scgd" );
        final ScoreCardModel model1 = GuidedScoreCardFactory.makeScoreCardWithCharacteristics( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing",
                                                                                               new ArrayList<Import>() {{
                                                                                                   add( new Import( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Applicant" ) );
                                                                                                   add( new Import( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Mortgage" ) );
                                                                                               }},
                                                                                               "scorecard1" );
        final String xml1 = GuidedScoreCardXMLPersistence.getInstance().marshal( model1 );
        ioService().write( path1,
                           xml1 );
        final Path path2 = basePath.resolve( "scorecard2.scgd" );
        final ScoreCardModel model2 = GuidedScoreCardFactory.makeScoreCardWithoutCharacteristics( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing",
                                                                                                  new ArrayList<Import>() {{
                                                                                                      add( new Import( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Applicant" ) );
                                                                                                      add( new Import( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Mortgage" ) );
                                                                                                  }},
                                                                                                  "scorecard2" );
        final String xml2 = GuidedScoreCardXMLPersistence.getInstance().marshal( model2 );
        ioService().write( path2,
                           xml2 );
        final Path path3 = basePath.resolve( "scorecard3.scgd" );
        final ScoreCardModel model3 = GuidedScoreCardFactory.makeEmptyScoreCard( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing",
                                                                                 "scorecard3" );
        final String xml3 = GuidedScoreCardXMLPersistence.getInstance().marshal( model3 );
        ioService().write( path3,
                           xml3 );

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        final Index index = getConfig().getIndexManager().get( org.uberfire.ext.metadata.io.KObjectUtil.toKCluster( basePath.getFileSystem() ) );

        //Score Cards using org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Applicant
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().addTerm( new ValueTypeIndexTerm( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Applicant" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 2,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path1 );
            assertContains( results,
                            path2 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

        //Score Cards using org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Mortgage
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().addTerm( new ValueTypeIndexTerm( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Mortgage" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 1,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path1 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

        //Score Cards using org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Mortgage#amount
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().addTerm( new ValueTypeIndexTerm( "org.drools.workbench.screens.guided.scorecard.backend.server.indexing.classes.Mortgage" ) ).addTerm( new ValueFieldIndexTerm( "amount" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 1,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path1 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

        //Score Cards using java.lang.Integer
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().addTerm( new ValueTypeIndexTerm( "java.lang.Integer" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 2,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path1 );
            assertContains( results,
                            path2 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

    }

    @Override
    protected TestIndexer getIndexer() {
        return new TestGuidedScoreCardFileIndexer();
    }

    @Override
    public Map<String, Analyzer> getAnalyzers() {
        return new HashMap<String, Analyzer>() {{
            put( RuleAttributeIndexTerm.TERM,
                 new RuleAttributeNameAnalyzer( LUCENE_40 ) );
        }};
    }

    @Override
    protected GuidedScoreCardResourceTypeDefinition getResourceTypeDefinition() {
        return new GuidedScoreCardResourceTypeDefinition();
    }

    @Override
    protected String getRepositoryName() {
        return this.getClass().getSimpleName();
    }

}
