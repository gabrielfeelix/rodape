package com.example.data

import com.example.data.api.OpenLibraryDoc

/**
 * Lista curada de livros populares/clássicos pra pré-preencher a tela de sugerir
 * (antes ela abria vazia com "comece a digitar"). São OpenLibraryDoc pra passarem
 * direto pelo fluxo de sugestão existente. Sem capa (coverI/isbn nulos) — a UI usa
 * a capa gerada por iniciais, então é INSTANTÂNEO e sem risco de capa errada. O
 * usuário rola (paginado) ou digita pra buscar qualquer outro livro.
 */
object PopularBooks {
    private fun b(title: String, author: String, year: Int? = null) =
        OpenLibraryDoc(title = title, authorName = listOf(author), firstPublishYear = year, coverI = null, isbn = null)

    val list: List<OpenLibraryDoc> = listOf(
        // --- Brasileiros / clássicos nacionais ---
        b("Dom Casmurro", "Machado de Assis", 1899),
        b("Memórias Póstumas de Brás Cubas", "Machado de Assis", 1881),
        b("O Cortiço", "Aluísio Azevedo", 1890),
        b("Capitães da Areia", "Jorge Amado", 1937),
        b("Gabriela, Cravo e Canela", "Jorge Amado", 1958),
        b("Grande Sertão: Veredas", "João Guimarães Rosa", 1956),
        b("Vidas Secas", "Graciliano Ramos", 1938),
        b("A Hora da Estrela", "Clarice Lispector", 1977),
        b("A Paixão Segundo G.H.", "Clarice Lispector", 1964),
        b("Iracema", "José de Alencar", 1865),
        b("O Guarani", "José de Alencar", 1857),
        b("Macunaíma", "Mário de Andrade", 1928),
        b("Quarto de Despejo", "Carolina Maria de Jesus", 1960),
        b("Torto Arado", "Itamar Vieira Junior", 2019),
        b("O Alienista", "Machado de Assis", 1882),
        b("Sagarana", "João Guimarães Rosa", 1946),
        // --- Clássicos mundiais ---
        b("Dom Quixote", "Miguel de Cervantes", 1605),
        b("Cem Anos de Solidão", "Gabriel García Márquez", 1967),
        b("Crime e Castigo", "Fiódor Dostoiévski", 1866),
        b("Os Irmãos Karamázov", "Fiódor Dostoiévski", 1880),
        b("Guerra e Paz", "Liev Tolstói", 1869),
        b("Anna Kariênina", "Liev Tolstói", 1877),
        b("Orgulho e Preconceito", "Jane Austen", 1813),
        b("O Morro dos Ventos Uivantes", "Emily Brontë", 1847),
        b("Jane Eyre", "Charlotte Brontë", 1847),
        b("Moby Dick", "Herman Melville", 1851),
        b("O Grande Gatsby", "F. Scott Fitzgerald", 1925),
        b("O Apanhador no Campo de Centeio", "J. D. Salinger", 1951),
        b("O Sol é para Todos", "Harper Lee", 1960),
        b("Cem Anos", "Ernest Hemingway", 1952),
        b("O Velho e o Mar", "Ernest Hemingway", 1952),
        b("1984", "George Orwell", 1949),
        b("A Revolução dos Bichos", "George Orwell", 1945),
        b("Admirável Mundo Novo", "Aldous Huxley", 1932),
        b("Fahrenheit 451", "Ray Bradbury", 1953),
        b("O Estrangeiro", "Albert Camus", 1942),
        b("A Metamorfose", "Franz Kafka", 1915),
        b("O Processo", "Franz Kafka", 1925),
        b("Ulisses", "James Joyce", 1922),
        b("Em Busca do Tempo Perdido", "Marcel Proust", 1913),
        b("Os Miseráveis", "Victor Hugo", 1862),
        b("O Conde de Monte Cristo", "Alexandre Dumas", 1844),
        b("Frankenstein", "Mary Shelley", 1818),
        b("Drácula", "Bram Stoker", 1897),
        b("O Retrato de Dorian Gray", "Oscar Wilde", 1890),
        b("O Pequeno Príncipe", "Antoine de Saint-Exupéry", 1943),
        // --- Fantasia / populares ---
        b("O Senhor dos Anéis", "J.R.R. Tolkien", 1954),
        b("O Hobbit", "J.R.R. Tolkien", 1937),
        b("Harry Potter e a Pedra Filosofal", "J.K. Rowling", 1997),
        b("As Crônicas de Nárnia", "C.S. Lewis", 1950),
        b("A Guerra dos Tronos", "George R.R. Martin", 1996),
        b("O Nome do Vento", "Patrick Rothfuss", 2007),
        b("Duna", "Frank Herbert", 1965),
        b("O Guia do Mochileiro das Galáxias", "Douglas Adams", 1979),
        b("A Menina que Roubava Livros", "Markus Zusak", 2005),
        b("O Caçador de Pipas", "Khaled Hosseini", 2003),
        b("O Alquimista", "Paulo Coelho", 1988),
        b("Cem Dias entre Céu e Mar", "Amyr Klink", 1985),
        b("Ensaio sobre a Cegueira", "José Saramago", 1995),
        b("O Amor nos Tempos do Cólera", "Gabriel García Márquez", 1985),
        b("A Casa dos Espíritos", "Isabel Allende", 1982),
        b("Norwegian Wood", "Haruki Murakami", 1987),
        b("Kafka à Beira-Mar", "Haruki Murakami", 2002),
        b("O Conto da Aia", "Margaret Atwood", 1985),
        b("Sapiens: Uma Breve História da Humanidade", "Yuval Noah Harari", 2011),
        b("A Coragem de Ser Imperfeito", "Brené Brown", 2012),
        b("O Poder do Hábito", "Charles Duhigg", 2012),
        b("Hábitos Atômicos", "James Clear", 2018),
        b("A Biblioteca da Meia-Noite", "Matt Haig", 2020),
        b("Mulherzinhas", "Louisa May Alcott", 1868),
        b("O Diário de Anne Frank", "Anne Frank", 1947),
    )
}
