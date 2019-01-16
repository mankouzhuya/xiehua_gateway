package com.xiehua.track.tree;

import com.xiehua.track.Span;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Tree implements Serializable{

    private Root root;

//    private List<Node<T>> nodes;
//
//
//    public void addNode(Node<T> node){
//        if(this.nodes == null){
//            synchronized (Tree.class){
//                this.nodes = new ArrayList<>();
//            }
//        }
//        nodes.add(node);
//    }








}
