package com.github.takahirom.appsearch_sample

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.GetByDocumentIdRequest
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.exceptions.AppSearchException
import androidx.appsearch.localstorage.LocalStorage
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context: Context = getApplicationContext()

        lifecycleScope.launch {
            val sessionFuture = LocalStorage.createSearchSession(
                LocalStorage.SearchContext.Builder(context, /*databaseName=*/"notes_app")
                    .build()
            )
            val setSchemaRequest = SetSchemaRequest.Builder().addDocumentClasses(Note::class.java)
                .build()
            val session = sessionFuture.await()
            session?.setSchema(setSchemaRequest)!!.await()
            val note = Note(
                namespace = "user1",
                id = "noteId",
                score = 10,
                text = "Buy fresh fruit"
            )

            val putRequest = PutDocumentsRequest.Builder().addDocuments(note).build()
            val result = session.put(putRequest).await()

            println("putRequest:$result")

            val document = session.getByDocumentId(
                GetByDocumentIdRequest
                    .Builder("user1")
                    .addIds("noteId")
                    .build()
            ).await()
            println(document)

            val searchSpec = SearchSpec.Builder()
                .addFilterNamespaces("user1")
                .build()

            val searchResult = session.search("fruit", searchSpec).nextPage.await()

            val genericDocument: GenericDocument = searchResult[0].genericDocument
            val schemaType = genericDocument.schemaType
            val searchedNote: Note? = try {
                if (schemaType == "Note") {
                    // Converts GenericDocument object to Note object.
                    genericDocument.toDocumentClass(Note::class.java)
                } else null
            } catch (e: AppSearchException) {
                e.printStackTrace()
                null
            }
            println(searchedNote)
        }


    }
}

@Document
data class Note(

    // Required field for a document class. All documents MUST have a namespace.
    @Document.Namespace
    val namespace: String,

    // Required field for a document class. All documents MUST have an Id.
    @Document.Id
    val id: String,

    // Optional field for a document class, used to set the score of the
    // document. If this is not included in a document class, the score is set
    // to a default of 0.
    @Document.Score
    val score: Int,

    // Optional field for a document class, used to index a note's text for this
    // document class.
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val text: String
)