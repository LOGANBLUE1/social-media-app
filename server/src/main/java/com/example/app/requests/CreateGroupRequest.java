package com.example.app.requests;

import lombok.Data;

import java.util.List;

/**
 * A new group chat. The creator comes from the principal and is added automatically, so memberIds
 * carries only the friends they picked -- including themselves is tolerated, not required.
 */
@Data
public class CreateGroupRequest {
    private String name;
    private List<Long> memberIds;
}
