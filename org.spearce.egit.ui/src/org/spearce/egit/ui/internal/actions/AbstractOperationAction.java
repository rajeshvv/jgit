package org.spearce.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;

public abstract class AbstractOperationAction implements IObjectActionDelegate
{
    private IWorkbenchPart wp;

    private IWorkspaceRunnable op;

    public void selectionChanged(final IAction act, final ISelection sel)
    {
        final List selection;
        if (sel instanceof IStructuredSelection && !sel.isEmpty())
        {
            selection = ((IStructuredSelection) sel).toList();
        }
        else
        {
            selection = Collections.EMPTY_LIST;
        }
        op = createOperation(act, selection);
        act.setEnabled(op != null && wp != null);
    }

    public void setActivePart(final IAction act, final IWorkbenchPart part)
    {
        wp = part;
    }

    protected abstract IWorkspaceRunnable createOperation(
        final IAction act,
        final List selection);

    protected void postOperation()
    {
    }

    public void run(final IAction act)
    {
        if (op != null)
        {
            try
            {
                try
                {
                    wp.getSite().getWorkbenchWindow().run(
                        true,
                        false,
                        new IRunnableWithProgress()
                        {
                            public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException
                            {
                                try
                                {
                                    op.run(monitor);
                                }
                                catch (CoreException ce)
                                {
                                    throw new InvocationTargetException(ce);
                                }
                            }
                        });
                }
                finally
                {
                    postOperation();
                }
            }
            catch (Throwable e)
            {
                final String msg = UIText.bind(
                    UIText.GenericOperationFailed,
                    act.getText());
                final IStatus status;

                if (e instanceof InvocationTargetException)
                {
                    e = e.getCause();
                }

                if (e instanceof CoreException)
                {
                    status = ((CoreException) e).getStatus();
                    e = status.getException();
                }
                else
                {
                    status = new Status(
                        IStatus.ERROR,
                        Activator.getPluginId(),
                        1,
                        msg,
                        e);
                }

                Activator.logError(msg, e);
                ErrorDialog.openError(
                    wp.getSite().getShell(),
                    act.getText(),
                    msg,
                    status,
                    status.getSeverity());
            }
        }
    }
}
