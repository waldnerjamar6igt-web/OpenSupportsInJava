package com.openSupports.demo.api.dto.knowledge;

import lombok.Data;

import java.util.List;

@Data
public class UpdateFolderOrderCmd {
    private List<Long> folderOrder;
}
