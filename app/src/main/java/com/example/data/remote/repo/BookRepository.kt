package com.example.data.remote.repo

import com.example.data.model.Book
import com.example.data.model.BookRating
import com.example.data.model.BookSummary
import com.example.data.model.Chapter
import com.example.data.model.ClubBook
import kotlinx.coroutines.flow.Flow

/**
 * Livros: catálogo (`books`), livros-do-clube (`club_books`), capítulos
 * (`chapters` + `chapter_templates`), resumos (`book_summaries`), avaliações
 * (`book_ratings`) e favoritos (`book_favorites`). Repo coeso (F3c) — junta o
 * que gira em torno do livro em vez de pulverizar.
 */
interface BookRepository {
    suspend fun insertBook(book: Book)
    suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String?
    suspend fun insertClubBook(clubBook: ClubBook)
    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>>
    fun getClubBooksFlow(clubId: String): Flow<List<Book>>
    suspend fun getClubBookStatus(clubId: String, bookId: String): String?
    suspend fun getBook(id: String): Book?
    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String)
    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?)
    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>>
    suspend fun deleteClubBook(clubId: String, bookId: String)
    suspend fun insertChapters(chapters: List<Chapter>)
    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>>
    suspend fun deleteChaptersForBook(bookId: String)
    suspend fun saveChapters(bookId: String, chapters: List<Chapter>)
    suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>?
    suspend fun shareChapterTemplate(isbn: String, tituloLivro: String, chapters: List<Pair<Int, String>>, userId: String)
    suspend fun insertBookSummary(summary: BookSummary)
    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?>
    suspend fun insertBookRating(rating: BookRating)
    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>>
    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?>
    suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean)
    fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean>
    suspend fun anyClubIdForBook(bookId: String): String?
    fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>>
}
