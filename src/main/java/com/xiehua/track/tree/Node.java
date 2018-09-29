package com.xiehua.track.tree;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Node<T> implements Serializable{

    private T date;

    private List<Node<T>> childs;

    public Node(T date) {
        this.date = date;
    }

    public void addChild(Node<T> node) {
        if (this.childs == null) {
            synchronized (Node.class) {
                this.childs = new ArrayList<>();
            }
        }
        this.childs.add(node);
    }


}
