/* 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.ticketlearner;

import static cn.zhuatech.ticketlearner.Checks.*;

import java.util.*;

/**
 * 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 用标注工单训练多项式朴素贝叶斯模型，对未标注工单预测类别、置信度及人工复核标志。
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Algorithm {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static List<String> tokens(String text) {
    var normalized =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    var out = new ArrayList<String>();
    var matcher =
        java.util.regex.Pattern.compile("\\p{IsHan}|[\\p{L}&&[^\\p{IsHan}]]+|\\p{Nd}+")
            .matcher(normalized);
    while (matcher.find()) out.add(matcher.group());
    return out;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static Map<String, Object> compute(Map<String, Object> cfg, List<Map<String, Object>> rows) {
    unique(rows);
    double threshold = num(cfg, "minConfidence", 0, 1);
    var words = new TreeMap<String, Map<String, Integer>>();
    var samples = new TreeMap<String, Integer>();
    var totals = new HashMap<String, Integer>();
    var vocab = new HashSet<String>();
    var pending = new ArrayList<Map<String, Object>>();
    for (var r : rows) {
      String label = str(r, "label", true).strip();
      var ts = tokens(str(r, "text", false));
      must(!ts.isEmpty(), "文本中无可用词项");
      if (label.isEmpty()) {
        pending.add(r);
        continue;
      }
      var counts = words.computeIfAbsent(label, k -> new HashMap<>());
      samples.merge(label, 1, Integer::sum);
      totals.merge(label, ts.size(), Integer::sum);
      for (String t : ts) {
        counts.merge(t, 1, Integer::sum);
        vocab.add(t);
      }
    }
    must(words.size() >= 2 && words.size() <= 40 && !pending.isEmpty(), "需要 2–40 个已标注类别及至少一条未标注工单");
    int train = rows.size() - pending.size();
    var out = new ArrayList<>();
    for (var r : pending) {
      var ts = tokens((String) r.get("text")).stream().filter(vocab::contains).toList();
      var scores = new TreeMap<String, Double>();
      String best = null;
      double max = -Double.MAX_VALUE;
      for (var cls : words.keySet()) {
        double score = Math.log((double) samples.get(cls) / train);
        for (String t : ts)
          score +=
              Math.log(
                  (words.get(cls).getOrDefault(t, 0) + 1.0) / (totals.get(cls) + vocab.size()));
        scores.put(cls, score);
        if (score > max) {
          best = cls;
          max = score;
        }
      }
      double sum = 0;
      for (double v : scores.values()) sum += Math.exp(v - max);
      double confidence = 1 / sum;
      out.add(
          Map.of(
              "id",
              r.get("id"),
              "predicted",
              best,
              "confidence",
              confidence,
              "knownTokens",
              ts.size(),
              "review",
              ts.isEmpty() || confidence < threshold));
    }
    return result(
        Map.of(
            "training",
            train,
            "predictions",
            pending.size(),
            "vocabulary",
            vocab.size(),
            "classes",
            words.size()),
        out);
  }
}
