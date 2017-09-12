// Generated code from Butter Knife. Do not modify!
package com.bridgefy.samples.fileshare;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FileActivity_ViewBinding implements Unbinder {
  private FileActivity target;

  @UiThread
  public FileActivity_ViewBinding(FileActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public FileActivity_ViewBinding(FileActivity target, View source) {
    this.target = target;

    target.recyclerView = Utils.findRequiredViewAsType(source, R.id.local_file_list, "field 'recyclerView'", RecyclerView.class);
    target.fab = Utils.findRequiredViewAsType(source, R.id.fab, "field 'fab'", FloatingActionButton.class);
    target.toolbar = Utils.findRequiredViewAsType(source, R.id.toolbar, "field 'toolbar'", Toolbar.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    FileActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.recyclerView = null;
    target.fab = null;
    target.toolbar = null;
  }
}
