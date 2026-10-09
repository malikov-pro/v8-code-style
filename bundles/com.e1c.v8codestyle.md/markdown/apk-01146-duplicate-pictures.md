# Common pictures with the identical content

Two or more common pictures of the configuration must not have the identical
content: a duplicated picture is edited in one place only, while the other
copies keep the outdated image.

For each common picture the hash sum (MD5) of its content is computed, and
pictures with the same hash sum are reported: every picture of a duplicate
group is reported and the message lists all pictures of the group. Pictures
with empty or not accessible content are not checked. Pictures adopted in
extension configurations are not checked.

Simplifications against the source algorithm: the content hash is computed
over the image files of the picture in the project (the multi-variant
`Picture.zip` container is hashed as a whole file, so its zip metadata may
mask a duplicate).

## Noncompliant Code Example

Two common pictures, `Picture1` and `Picture2`, whose image files are byte
identical.

## Compliant Solution

A single common picture referenced from all places of use.

## See also

- [Standard 440. Using duplicate code](https://its.1c.ru/db/v8std#content:440:hdoc)
- Port of the АПК rule `АПК_01146` (error 1343).
- Checks are not applied to objects adopted in extension configurations.
