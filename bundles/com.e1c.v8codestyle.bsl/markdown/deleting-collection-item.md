# Deleting collection item while iterating

Checks that items of a collection are not deleted while the collection is iterated with the **For Each ... In ... Do** loop: deleting an item shifts the index of the next one, so some items will be skipped.

## Noncompliant Code Example

```bsl
For Each Item In Collection Do
    
    Collection.Delete(Item);
    
EndDo;
```

## Compliant Solution

Collect the items to delete into a temporary collection and delete them after the loop (or iterate the loop in reverse order by index).

## See

- [BSL Language Server: DeletingCollectionItem](https://1c-syntax.github.io/bsl-language-server/diagnostics/DeletingCollectionItem/)
- Ported from the BSL Language Server diagnostic `DeletingCollectionItem`.
