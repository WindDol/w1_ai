package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.retrieval.SourceTextBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SourcePageLocatorUnitTest {

    @Test
    void locatesChunkPagesFromMonkeyOcrTextBlocks() {
        SourcePageLocator locator = new SourcePageLocator(List.of(
                new SourceTextBlock("A sufficiently long introduction text that belongs to the first page.", 0),
                new SourceTextBlock("The stability criterion follows from the spectrum of the operator.", 1)
        ));

        SourcePageLocator.PageLocation location = locator.locate(
                "The stability criterion follows from the spectrum of the operator.");

        assertEquals(2, location.start());
        assertEquals(2, location.end());
    }

    @Test
    void leavesPageUnknownWhenArtifactTextCannotBeAligned() {
        SourcePageLocator locator = new SourcePageLocator(List.of(
                new SourceTextBlock("Original source text on the first page.", 0)));

        SourcePageLocator.PageLocation location = locator.locate(
                "Completely different normalized content that is not present in the source.");

        assertNull(location.start());
        assertNull(location.end());
    }
}
