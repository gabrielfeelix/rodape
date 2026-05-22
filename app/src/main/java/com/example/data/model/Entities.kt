package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val nome: String,
    val email: String,
    val avatarUrl: String
)

@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey val id: String,
    val nome: String,
    val descricao: String,
    val codigo: String,
    val cor: String, // swatch color index or hex
    val privacidade: String, // "convidados" | "publico"
    val criadorId: String,
    val criadoEm: Long
)

@Entity(tableName = "club_members", primaryKeys = ["clubId", "userId"])
data class ClubMember(
    val clubId: String,
    val userId: String,
    val papel: String, // "admin" | "member"
    val entrouEm: Long
)

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val openlibraryId: String,
    val isbn: String
)

@Entity(tableName = "club_books", primaryKeys = ["clubId", "bookId"])
data class ClubBook(
    val clubId: String,
    val bookId: String,
    val status: String, // "current" | "finished" | "suggested" | "next"
    val ordem: Int
)

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey val id: String,
    val bookId: String,
    val numero: Int,
    val titulo: String
)

@Entity(tableName = "user_progress", primaryKeys = ["userId", "clubId", "bookId"])
data class UserProgress(
    val userId: String,
    val clubId: String,
    val bookId: String,
    val currentChapter: Int
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val chapterId: String,
    val clubId: String,
    val userId: String,
    val texto: String,
    val criadoEm: Long
)

@Entity(tableName = "reactions", primaryKeys = ["commentId", "userId", "emoji"])
data class Reaction(
    val commentId: String,
    val userId: String,
    val emoji: String
)

@Entity(tableName = "votes", primaryKeys = ["clubBookId", "userId"])
data class Vote(
    val clubBookId: String,
    val userId: String,
    val votedAt: Long
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey val id: String,
    val clubId: String,
    val data: String,
    val hora: String,
    val local: String,
    val agenda: String
)

@Entity(tableName = "meeting_rsvps", primaryKeys = ["meetingId", "userId"])
data class MeetingRsvp(
    val meetingId: String,
    val userId: String,
    val status: String // "Vou" | "Talvez" | "Não vou"
)

@Entity(tableName = "notifications")
data class DbNotification(
    @PrimaryKey val id: String,
    val userId: String,
    val clubId: String,
    val tipo: String,
    val payloadJson: String,
    val lida: Boolean,
    val criadoEm: Long
)
