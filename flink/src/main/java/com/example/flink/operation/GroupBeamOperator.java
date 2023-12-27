package com.example.flink.operation;

import com.example.flink.data.BeamData;
import com.example.flink.data.ChannelBeamData;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class GroupBeamOperator
    extends ProcessWindowFunction<ChannelBeamData, BeamData, Integer, TimeWindow> {
  private List<OutputTag<BeamData>> outputTagList;

  @Builder
  @Jacksonized
  public GroupBeamOperator(List<String> algorithmNameList) {
    outputTagList =
        algorithmNameList.stream()
            .map(algorithmName -> new OutputTag<BeamData>(algorithmName){})
            .collect(Collectors.toList());
  }

  @Override
  public void process(
      Integer integer,
      ProcessWindowFunction<ChannelBeamData, BeamData, Integer, TimeWindow>.Context context,
      Iterable<ChannelBeamData> elements,
      Collector<BeamData> collector)
      throws Exception {
    BeamData beamData =
        BeamData.builder()
            // TODO construct BeamData
            .build();
    for (OutputTag<BeamData> outputTag : outputTagList) {
      context.output(outputTag, beamData);
    }
  }
}
