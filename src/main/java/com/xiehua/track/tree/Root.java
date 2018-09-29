package com.xiehua.track.tree;

import com.xiehua.track.Span;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class Root implements Serializable{

    private Span rootSpan;

}
