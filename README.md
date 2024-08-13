# 1. AnySeparatorLineIterator

## 1.1 介绍

以任意 not empty 字符串作为分隔符的行迭代器。

# 2. SheetReader & ExcelReader

## 2.1 介绍

比 hutool 更快地读取 excel。

## 2.2 背景

1. 读取含有大量合并单元格的 excel；
2. 但是 hutool 每次处理一个 cell 的时候都会获取一次合并单元格信息，复杂度 n * m。

## 2.3 分析 & 实现

1. 获取 1 次合并单元格信息就够了；
2. 读取 excel 时一般都是从上到下、从左到右；
3. 结合前 2 点，可以维护一个 tree map，让最左上角的合并单元格信息在最前面；
    1. key 是合并单元格中最左上角单元格的坐标；
    2. value 是合并单元格中涵盖单元格的个数，每遍历到一次就减一，为零时将 entry 从 tree map 中移除。
4. 遍历时，如果确定当前单元格在当前合并单元格的左上角，那么它必不可能在之后的任意一个合并单元格。

## 2.4 性能

1. 因为是从上到下、从左到右，迭代 tree map 的时候，大多数情况下只需几次就能很快确定。

## 2.5 使用限制

1. 只适用于顺序读取 excel 的情况；
2. excel 不会被意外修改。

# 3 ReplacedLineReader & ReplacedLineInputStream

## 3.1 说明

在不改变原文件的情况下，读取原始行，调用 LineReplacer 之后，将替换行交给调用者。
