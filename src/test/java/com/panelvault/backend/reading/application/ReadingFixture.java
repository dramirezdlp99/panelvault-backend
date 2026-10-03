package com.panelvault.backend.reading.application;

import com.panelvault.backend.identity.application.MutableClock;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.application.InMemoryComicRepository;
import com.panelvault.backend.library.application.LibraryService;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicSamples;
import java.time.Instant;
import java.util.UUID;

/** Arma biblioteca y lectura en memoria, conectadas como en produccion (Test Fixture). */
public class ReadingFixture {

    public static final Instant START = Instant.parse("2026-10-03T15:00:00Z");

    public final MutableClock clock = new MutableClock(START);
    public final InMemoryComicRepository comics = new InMemoryComicRepository();
    public final InMemoryReadingProgressRepository progress = new InMemoryReadingProgressRepository();
    public final InMemoryBookmarkRepository bookmarks = new InMemoryBookmarkRepository();
    public final ReadingStatsService stats = new ReadingStatsService(progress);
    public final LibraryService library = new LibraryService(comics, stats, clock);
    public final ReadingService reading = new ReadingService(progress, comics, clock);
    public final BookmarkService bookmarkService = new BookmarkService(bookmarks, comics, clock);

    private int seed;

    /** Agrega un comic de {@code pages} paginas a la biblioteca del usuario. */
    public ComicId comicDe(UserId owner, int pages) {
        ComicId id = new ComicId(UUID.randomUUID());
        seed++;
        library.save(owner, id, ComicSamples.details("Comic " + seed, pages, seed));
        return id;
    }
}
