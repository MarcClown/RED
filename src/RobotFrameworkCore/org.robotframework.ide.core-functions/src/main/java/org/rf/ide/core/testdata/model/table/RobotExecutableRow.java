/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.FileFormat;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.ICommentHolder;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.table.exec.descs.ExecutableRowDescriptorBuilder;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.tasks.Task;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class RobotExecutableRow<T> extends AModelElement<T> implements ICommentHolder, Serializable {

    private static final long serialVersionUID = -4158729064542423691L;

    private final static Pattern TSV_COMMENT = Pattern.compile("(\\s)*\"(\\s)*[#].*\"(\\s)*$");

    private RobotToken action;

    private final List<RobotToken> arguments = new ArrayList<>();

    private final List<RobotToken> comments = new ArrayList<>();

    public RobotExecutableRow() {
        this.action = new RobotToken();
    }

    @Override
    public void setParent(final T parent) {
        super.setParent(parent);
        fixMissingTypes();
    }

    public RobotToken getAction() {
        fixMissingTypes();
        return action;
    }

    public void setAction(final RobotToken action) {
        this.action = updateOrCreate(this.action, action, getActionType());

        fixMissingTypes();
    }

    private IRobotTokenType getActionType() {
        final T parent = getParent();
        if (parent != null && parent.getClass() == TestCase.class) {
            return RobotTokenType.TEST_CASE_ACTION_NAME;
        } else if (parent != null && parent.getClass() == Task.class) {
            return RobotTokenType.TASK_ACTION_NAME;
        } else if (parent != null && parent.getClass() == UserKeyword.class) {
            return RobotTokenType.KEYWORD_ACTION_NAME;
        }
        return RobotTokenType.UNKNOWN;
    }

    private IRobotTokenType getArgumentType() {
        final T parent = getParent();
        if (parent != null && parent.getClass() == TestCase.class) {
            return RobotTokenType.TEST_CASE_ACTION_ARGUMENT;
        } else if (parent != null && parent.getClass() == Task.class) {
            return RobotTokenType.TASK_ACTION_ARGUMENT;
        } else if (parent != null && parent.getClass() == UserKeyword.class) {
            return RobotTokenType.KEYWORD_ACTION_ARGUMENT;
        }
        return RobotTokenType.UNKNOWN;
    }

    public List<RobotToken> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public void setArgument(final int index, final String argument) {
        final RobotToken token = new RobotToken();
        token.setText(argument);

        setArgument(index, token);
    }

    public void setArgument(final int index, final RobotToken argument) {
        updateOrCreateTokenInside(arguments, index, argument, getArgumentType());

        fixMissingTypes();
    }

    public void addArgument(final int index, final RobotToken argument) {
        fixForTheType(argument, getArgumentType(), true);
        arguments.add(index, argument);

        fixMissingTypes();
    }

    public void addArgument(final RobotToken argument) {
        fixForTheType(argument, getArgumentType(), true);
        arguments.add(argument);

        fixMissingTypes();
    }

    public void removeArgument(final int index) {
        arguments.remove(index);
    }

    @Override
    public void addCommentPart(final RobotToken rt) {
        fixComment(getComment(), rt);
        this.comments.add(rt);
    }

    @Override
    public List<RobotToken> getComment() {
        return Collections.unmodifiableList(comments);
    }

    @Override
    public void setComment(final String comment) {
        final RobotToken tok = new RobotToken();
        tok.setText(comment);
        setComment(tok);
    }

    @Override
    public void setComment(final RobotToken comment) {
        this.comments.clear();
        addCommentPart(comment);
    }

    @Override
    public void removeCommentPart(final int index) {
        this.comments.remove(index);
    }

    @Override
    public void clearComment() {
        this.comments.clear();
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public RobotToken getDeclaration() {
        return action;
    }

    @Override
    public ModelType getModelType() {
        final List<IRobotTokenType> types = getAction().getTypes();
        if (types.contains(RobotTokenType.TEST_CASE_ACTION_NAME)) {
            return ModelType.TEST_CASE_EXECUTABLE_ROW;
        } else if (types.contains(RobotTokenType.TASK_ACTION_NAME)) {
            return ModelType.TASK_EXECUTABLE_ROW;
        } else if (types.contains(RobotTokenType.KEYWORD_ACTION_NAME)) {
            return ModelType.USER_KEYWORD_EXECUTABLE_ROW;
        } else if (types.contains(RobotTokenType.UNKNOWN)) {
            final AModelElement<?> parent = (AModelElement<?>) getParent();
            if (parent != null) {
                final ModelType parentType = parent.getModelType();
                if (parentType == ModelType.TEST_CASE) {
                    return ModelType.TEST_CASE_EXECUTABLE_ROW;
                } else if (parentType == ModelType.TASK) {
                    return ModelType.TASK_EXECUTABLE_ROW;
                } else if (parentType == ModelType.USER_KEYWORD) {
                    return ModelType.USER_KEYWORD_EXECUTABLE_ROW;
                }
            }
        }

        return ModelType.UNKNOWN;
    }

    @Override
    public FilePosition getBeginPosition() {
        FilePosition position = getAction().getFilePosition();
        if (position.isNotSet()) {
            // this may be comment row
            final List<RobotToken> tokens = getElementTokens();
            if (tokens.size() > 1) {
                position = tokens.get(1).getFilePosition();
            }
        }
        return position;
    }

    @Override
    public List<RobotToken> getElementTokens() {
        fixMissingTypes();
        final List<RobotToken> tokens = new ArrayList<>();
        tokens.add(getAction());
        tokens.addAll(compact(arguments));
        tokens.addAll(compact(comments));

        return tokens;
    }

    private List<RobotToken> compact(final List<RobotToken> elementsSingleType) {
        final int size = elementsSingleType.size();
        for (int i = size - 1; i >= 0; i--) {
            if (elementsSingleType.size() == 0) {
                break;
            }

            final RobotToken t = elementsSingleType.get(i);
            if (t.getText() == null || t.getText().isEmpty()) {
                elementsSingleType.remove(i);
            } else {
                break;
            }
        }

        return elementsSingleType;
    }

    public boolean isExecutable() {
        if (action == null) {
            return false;
        }
        if (getParent() instanceof IExecutableStepsHolder) {
            @SuppressWarnings("unchecked")
            final IExecutableStepsHolder<AModelElement<? extends ARobotSectionTable>> parent = (IExecutableStepsHolder<AModelElement<? extends ARobotSectionTable>>) getParent();
            final FileFormat fileFormat = parent.getHolder().getParent().getParent().getParent().getFileFormat();

            if (!action.getTypes().contains(RobotTokenType.START_HASH_COMMENT)) {
                final String text = action.getText().trim();
                final List<RobotToken> elementTokens = getElementTokens();
                if (text.equals("\\")) {
                    return elementTokens.size() > 1
                            && !elementTokens.get(1).getTypes().contains(RobotTokenType.START_HASH_COMMENT);

                } else if ("".equals(text)) {
                    if (fileFormat == FileFormat.TSV) {
                        return elementTokens.size() > 1
                                && !elementTokens.get(1).getTypes().contains(RobotTokenType.START_HASH_COMMENT);

                    } else if (action.getTypes().contains(RobotTokenType.FOR_WITH_END_CONTINUATION)) {
                        return true;
                    }
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        } else {
            return !action.getTypes().contains(RobotTokenType.START_HASH_COMMENT);
        }
    }

    public static boolean isTsvComment(final String text, final FileFormat format) {
        return format == FileFormat.TSV && TSV_COMMENT.matcher(text).matches();
    }

    public IExecutableRowDescriptor<T> buildLineDescription() {
        return new ExecutableRowDescriptorBuilder().buildLineDescriptor(this);
    }

    @Override
    public boolean removeElementToken(final int index) {
        return super.removeElementFromList(arguments, index);
    }

    @Override
    public void insertValueAt(final String value, final int position) {
        final RobotToken tokenToInsert = new RobotToken();
        tokenToInsert.setText(value);
        if (position == 0) { // new action
            if (action.isNotEmpty() || !arguments.isEmpty()) { // in case of artificial comment action token
                arguments.add(0, action);
            } else if (value.isEmpty()) {
                tokenToInsert.setText("\\");
            }
            action = tokenToInsert;
        } else if (arguments.isEmpty() && !action.isNotEmpty()) { // whole line comment
            comments.add(position, tokenToInsert);
        } else if (position - 1 <= arguments.size()) { // new argument
            if (position - 1 == arguments.size() && value.isEmpty()) {
                tokenToInsert.setText("\\");
            }
            arguments.add(position - 1, tokenToInsert);
        } else if (position - 1 - arguments.size() <= comments.size()) { // new comment part
            comments.add(position - 1 - arguments.size(), tokenToInsert);
        }
        fixMissingTypes();
    }

    public <P> RobotExecutableRow<P> copy() {
        final RobotExecutableRow<P> execRow = new RobotExecutableRow<>();
        execRow.setAction(getAction().copyWithoutPosition());
        for (final RobotToken arg : getArguments()) {
            execRow.addArgument(arg.copyWithoutPosition());
        }

        for (final RobotToken cmPart : getComment()) {
            execRow.addCommentPart(cmPart.copyWithoutPosition());
        }

        return execRow;
    }

    private void fixMissingTypes() {
        if (getParent() != null) {
            if (action != null && (getArguments().size() > 0 || action.isNotEmpty() || getComment().size() > 0)) {
                action.getTypes().remove(RobotTokenType.UNKNOWN);
                fixForTheType(action, getActionType(), true);
            }

            for (int i = 0; i < getArguments().size(); i++) {
                final RobotToken token = getArguments().get(i);
                token.getTypes().remove(RobotTokenType.UNKNOWN);
                token.getTypes().remove(getActionType());
                fixForTheType(token, getArgumentType(), true);
            }
        }
    }
}
