package com.xiehua.mvc.controller.dto;

import com.xiehua.cache.dto.SimpleKvDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BroadcastUserPermissionsDTO implements Serializable {

    private String gid;

    private List<SimpleKvDTO> userPermissions;
}
