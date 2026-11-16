/* 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.ticketlearner;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

/** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
@SuppressWarnings("unchecked")
class AlgorithmTest {
  Map<String, Object> cfg;
  List<Map<String, Object>> data;

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @BeforeEach
  void load() throws Exception {
    var fixture =
        new ObjectMapper()
            .readValue(getClass().getResourceAsStream("/static/catalog.json"), Map.class);
    cfg = (Map<String, Object>) fixture.get("settings");
    data = (List<Map<String, Object>>) fixture.get("rows");
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void boundaryCases() {
    assertThrows(
        Model.Failure.class,
        () ->
            Algorithm.compute(
                cfg,
                List.of(
                    Map.of("id", "1", "label", "x", "text", "包裹"),
                    Map.of("id", "2", "label", "", "text", "包裹"))));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void enterpriseEvidenceAndBoundaries() {
    var result=Algorithm.compute(Map.of("minConfidence",0),List.of(
        Map.of("id","a","label","A","text","shared"),Map.of("id","b","label","B","text","shared"),
        Map.of("id","x","label","","text","shared"),Map.of("id","y","label","","text","unknown")));
    var rows=(List<Map<String,Object>>)result.get("rows");
    assertEquals(true,rows.get(0).get("review"));assertEquals("TIED",rows.get(0).get("reviewReason"));
    assertEquals(0.0,((Number)rows.get(0).get("confidenceGap")).doubleValue(),1e-10);
    assertEquals("NO_EVIDENCE",rows.get(1).get("reviewReason"));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void knownResults() {
    var result = Algorithm.compute(cfg, data);
    var m = (Map<String, Object>) result.get("metrics");
    var out = (List<Map<String, Object>>) result.get("rows");
    var first = out.getFirst();
    assertEquals(3, ((Number) m.get("training")).intValue());
    assertEquals("物流", first.get("predicted"));
    var rs = new ArrayList<>(data);
    rs.set(3, Map.of("id", "T4", "text", "xyzzy", "label", ""));
    assertEquals(
        true,
        ((List<Map<String, Object>>) Algorithm.compute(cfg, rs).get("rows"))
            .getFirst()
            .get("review"));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void rejectsInvalidInput() {
    cfg.put("minConfidence", 2);
    assertThrows(Model.Failure.class, () -> Algorithm.compute(cfg, data));
  }
}
