package cn.winddol.ai.test;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.entity.SectionPO;
import cn.winddol.ai.infrastructure.parser.MarkdownParser;
import cn.winddol.ai.types.common.utils.FingerprintUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@SpringBootTest
public class TestMarkdownParser {
    @Resource
    private ISymbolExtractor extractor;

    @Test
    public void test() throws Exception {
        // 1. 读取昨天的文件
        String filePath = "C:\\Users\\Mr Ding.LAPTOP-H54HCE12\\Desktop\\ai_pdf\\python\\output_test.md"; // 确保路径对
        String markdown = Files.readString(Paths.get(filePath));

        // 2. 解析
        MarkdownParser parser = new MarkdownParser();
        List<SectionPO> sections = parser.parse(markdown);

        // 3. 打印结果验证
        System.out.println("====== 解析结果 ======");
        System.out.println("共发现章节数: " + sections.size());

        for (SectionPO sec : sections) {
            System.out.println("------------------------------------------------");
            System.out.println("UUID: " + sec.uuid);
            System.out.println("Header: " + "#".repeat(sec.level) + " " + sec.header);
            System.out.println("Content Length: " + sec.getContent().length());
            // 打印前 50 个字看看内容对不对
            String preview = sec.getContent().length() > 50 ? sec.getContent().substring(0, 50) : sec.getContent();
            System.out.println("Content Preview: " + preview.replace("\n", " "));
        }

        // 4. 构建 Outline JSON
        System.out.println("\n====== Outline JSON (存入 papers 表) ======");
        System.out.println(parser.buildFlatOutline(sections).toString());
    }

    @Resource
    private IPaperRepository repository;

    @Test
    public void testInsert() throws Exception {
        // 1. 读取并解析文件 (Day 2 的代码)
        String markdown = Files.readString(Paths.get("C:\\Users\\Mr Ding.LAPTOP-H54HCE12\\Desktop\\ai_pdf\\python\\output_test.md"));
        MarkdownParser parser = new MarkdownParser();
        List<SectionPO> pos = parser.parse(markdown);
        String headerContent = pos.get(0).getContent() + (pos.size() > 1 ? pos.get(1).getContent() : "");
        RefMetadata paperMeta = extractor.extractRefMetadata(headerContent);
        if (paperMeta == null || paperMeta.getAuthorSurnames().isEmpty() || paperMeta.getYear() == 0) {
            log.error("❌ Metadata extraction failed. Aborting ingestion to prevent library pollution.");
            return;
        }
        String fingerprint = FingerprintUtils.generateRefFingerprint(
                paperMeta.getAuthorSurnames(),
                paperMeta.getYear()
        );
        // 2. 调用 Service 入库
        String title = "Identical phase oscillators with global sinusoidal coupling evolve by Möbius group action";
        repository.saveFullPaper(title, pos,fingerprint);

        System.out.println("入库成功！请在 DBeaver 中查看数据。");
    }

    @Test
    public void  testF() throws Exception{
        String a = """
                Chaos
                An Interdisciplinary Journal of Nonlinear Science
                
                RESEARCH ARTICLE | OCTOBER 15 2009
                
                Seth A. Marvel; Renato E. Mirollo; Steven H. Strogatz
                Check for updates
                https://doi.org/10.1063/1.3247089
                View Online | Export Citation
                27 August 2024 00:59:50
                Seth A. Marvel,$^{1,a)}$ Renato E. Mirollo,$^2$ and Steven H. Strogatz$^1$
                $^1$*Center for Applied Mathematics, Cornell University, Ithaca, New York 14853, USA*
                $^2$*Department of Mathematics, Boston College, Chestnut Hill, Massachusetts 02167, USA*
                (Received 23 July 2009; accepted 21 September 2009; published online 15 October 2009)
                Large arrays of coupled limit-cycle oscillators have been used to model diverse systems in physics, biology, chemistry, engineering, and social science. The special case of phase oscillators coupled all to all through sinusoidal interactions has attracted mathematical interest because of its analytical tractability. About 20 years ago, numerical experiments revealed that when the oscillators are all identical, these systems display an exceptionally simple form of collective behavior: For all $N \\geq 3$, where $N$ is the number of oscillators, all trajectories are confined to manifolds with $N-3$ fewer dimensions than the state space itself. Several insights have been obtained over the past two decades, but it has remained an open problem to pinpoint the symmetry or other structure that causes this nongeneric behavior. Here we show that group theory provides the explanation: The governing equations for these systems arise naturally from the action of the group of conformal mappings of the unit disk to itself. This link unifies and explains the previous numerical and analytical results, and yields new constants of motion for this class of dynamical systems.
                """;
        String b = """
                When a nonlinear system shows unexpectedly simple behavior, it may be a clue that some hidden structure awaits discovery. For example, recall the classic detective story$^1$ that began in the 1950s with the work of Fermi, Pasta, and Ulam.$^{2-4}$ In their numerical simulations of a chain of anharmonic oscillators, Fermi *et al.* were surprised to find the chain returning almost perfectly, again and again, to its initial state. The struggle to understand these recurrences led Zabusky and Kruskal$^5$ to the discovery of solitons in the Korteweg–deVries equation, which in turn sparked a series of results showing that this equation possessed many conserved quantities—in fact, infinitely many.$^6$ Then several other equations turned out to have the same properties. At the time these results seemed almost miraculous. But by the mid-1970s the hidden structure responsible for all of them—the complete integrability of certain infinite-dimensional Hamiltonian systems$^7$—had been made manifest by the inverse scattering transform$^{8,9}$ and Lax$^{10}$ pairs.
                Something similar, although far less profound, has been happening again in nonlinear science. The broad topic is still coupled oscillators, but unlike the conservative oscillators studied by Fermi *et al.*, the oscillators in question now are dissipative and have stable limit cycles. This latest story began around 1990 when a few researchers noticed an enormous amount of neutral stability and seemingly low-dimensional behavior in their simulations of Josephson junction arrays—specifically, arrays of identical, overdamped junctions arranged in series and coupled through a common load.$^{11-15}$ Then, just a year ago, Antonsen *et al.*$^{16}$ uncovered similarly low-dimensional dynamics in the periodically forced version of the Kuramoto model of biological oscillators.$^{17-19}$ This was particularly surprising because the oscillators in that model are nonidentical.
                As in the soliton story, these numerical observations then inspired a series of theoretical advances. For the case of identical oscillators (the subject of this paper), these included the discovery of constants of motion$^{20,21}$ and of a pair of transformations that established the low dimensionality of the dynamics.$^{20-25}$ But what remained to be found was the final piece, the identification of the hidden structure. Without it, it was unclear why the transformations and constants of motion should exist in the first place.
                In this paper we show that the group of Möbius transformations is the key to understanding this class of dynamical systems. Our analysis unifies the previous treatments of
                $^a$)Electronic mail: sam255@cornell.edu.
                Josephson arrays and the Kuramoto model, and clarifies the geometric and algebraic structures responsible for their low-dimensional behavior. One spinoff of our approach is a new set of constants of motion; these generalize the constants found previously and hold for a wider class of oscillator arrays.
                The paper is organized as follows. To keep the treatment self-contained and to establish notation, Sec. II reviews the relevant background about coupled oscillators and the Möbius group. In Sec. III we show how to use Möbius transformations to reduce the dynamics of identical oscillators with global sinusoidal coupling, the type of coupling that appears in both the Josephson and Kuramoto models. The reduced flow lives on a set of invariant three-dimensional manifolds, arising naturally as the so-called group orbits of the Möbius group. The results obtained in this way are then compared with previous findings (Sec. IV) and used to generate new constants of motion via the classical cross ratio construction (Sec. V). We explore the dynamics on the invariant manifolds in Sec. VI and show that the phase portraits for resistively coupled Josephson arrays are filled with chaos and island chains, reminiscent of the pictures encountered in Hamiltonian chaos and Kolmogorov–Arnold–Moser theory.        
                """;
        String headerContent = a + b;
        RefMetadata paperMeta = extractor.extractRefMetadata(headerContent);
        String fingerprint = FingerprintUtils.generateRefFingerprint(
                paperMeta.getAuthorSurnames(),
                paperMeta.getYear()
        );

    }
}
