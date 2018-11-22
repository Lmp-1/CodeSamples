using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class HorizontalScrollSnap : MonoBehaviour
{
	public RectTransform content;
	public RectTransform center;

	public int predefinedItemSize = 0;
	public float spacing = 0;

	public bool snapEndbled = true;
	public float snapSpeed = 2.5f;
	public float snapToPreviousWhenPassed = 0.1f;

	public bool scaling = false;
	public float minScale = 0.8f;
	public float maxScale = 1f;

	public bool stopScrollAtNextElement = true;
	public bool controlledFromOutside = false;
	public TabScript parentTabScript;

	private ScrollRect scrollRect;
	public float itemDistance { get; private set; }

	private RectTransform[] items;

	public int itemCount { get; private set; }
	public int closestItemIndex { get; private set; }
	public int closestSecondItemIndex { get; private set; }
	private float repositionAt = 1500f;
	public float[] distance { get; private set; }
	public float[] distReposition { get; private set; }

	private bool dragging = false;
	private bool clickedSidePanel = false;
	private int startedDragAtItemIndex = -1;
	private int moveToItem = -1;

	private bool initDone = false;
	private int copiesOfContent = 1;

	private List<GameObject> newChildren = null;

	void Awake()
	{
		if (parentTabScript == null || !controlledFromOutside)
		{
			OnAwake();
		}
	}

	public void OnAwake()
	{
		scrollRect = gameObject.GetComponent<ScrollRect>();

		if (center == null)
		{
			GameObject centerToCompare = transform.Find("CenterToCompare").gameObject;

			if (centerToCompare != null)
			{
				center = centerToCompare.GetComponent<RectTransform>();
			} else
			{
				center = gameObject.GetComponent<RectTransform>();
			}
		}
	}

	void Update()
	{
		if (parentTabScript == null && !controlledFromOutside)
		{
			OnUpdate();
		}
	}

	public GameObject GetClosestItemGO()
	{
		return items [closestItemIndex].gameObject;
	}

	public bool CheckMovement()
	{
		return (!dragging && moveToItem == -1 && distance != null && distance.Length > closestItemIndex && Mathf.Abs (distance[closestItemIndex]) <= 0.0002f);
	}

	public void OnUpdate()
	{
		if (!initDone || items == null || items.Length == 0) return;

		CalculateDistances();

		if (!clickedSidePanel)
		{
			FindClosestButtonIndex();
			FindClosestSecondButtonIndex();
		}

		if (snapEndbled && !dragging)
		{
			if (moveToItem != -1)
			{
				DoLerp(-items[moveToItem].anchoredPosition.x);
			} else
			{
				LerpToItem(-items[closestItemIndex].anchoredPosition.x);
			}
		}

		UpdateFromNewChildren();
	}

	private void UpdateFromNewChildren()
	{
		if (newChildren != null && newChildren.Count > 0)
		{
			Clear();
			itemCount = 0;
			List<GameObject> newChildrenList = newChildren;
			newChildren = null;
			SetData(newChildrenList);
		}
	}

	private bool hasOldChildren(List<GameObject> itemsList)
	{
		if (initDone && content.childCount > 0)
		{
			newChildren = itemsList;
			return true;
		}

		return false;
	}

	public void SetData(List<GameObject> itemsList)
	{
		if (hasOldChildren(itemsList))
		{
			return;
		}

		if (itemsList == null || itemsList.Count == 0) 
		{
			return;
		}

		initDone = false;

		itemCount = itemsList.Count;
		closestItemIndex = 0;
		closestSecondItemIndex = 0;
		InitArrays();
		InitObjects(itemsList);
	}

	public void SetDataFromChildren()
	{
		if (content.childCount == 0) return;

		List<GameObject> itemsList = new List<GameObject>(content.childCount);
		for (int i = 0; i < content.childCount; i++)
		{
			itemsList.Add(content.GetChild(i).gameObject);
		}

		content.DetachChildren();
		SetData(itemsList);
	}

	private void InitArrays()
	{
		distance = new float[itemCount];
		distReposition = new float[itemCount];
	}

	private void InitObjects(List<GameObject> itemsList)
	{
		GameObject[] itemObjects = new GameObject[itemCount];
		RectTransform[] itemRectTransforms = new RectTransform[itemCount];

		GameObject item;

		if (predefinedItemSize > 0)
		{
			itemDistance = predefinedItemSize + spacing;
			SetContentSize();
			InitRepositionDistance();

			for (int i = 0; i < itemCount; i++)
			{
				item = InitObject(itemsList[i], i);
				itemRectTransforms[i] = item.GetComponent<RectTransform>();
				itemRectTransforms[i].anchoredPosition = new Vector2(itemDistance * i, itemRectTransforms[i].anchoredPosition.y);
			}

			initDone = true;
		} else
		{
			for (int i = 0; i < itemCount; i++)
			{
				item = InitObject(itemsList[i], i);
				itemRectTransforms[i] = item.GetComponent<RectTransform>();
			}

			StartCoroutine(InitItemDistance());
		}

		items = itemRectTransforms;
	}

	private void SetContentSize()
	{
		content.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, itemDistance * itemCount);
	}

	private GameObject InitObject(GameObject newGO, int index)
	{
		newGO.transform.SetParent(content);
		newGO.transform.localPosition = new Vector3(0f, 0f, 0f);
		newGO.transform.localScale = Vector3.one;

		CheckScrollSnapAdditionalScripts(newGO, index);

		return newGO;
	}

	private void CheckScrollSnapAdditionalScripts(GameObject newGO, int index)
	{
		CheckTabHeaderScript(newGO, index);
		CheckCanvasGroupScript(newGO, index);
		CheckNestedScrollRectScript(newGO, index);
	}

	private void CheckTabHeaderScript(GameObject newGO, int index)
	{
		TabHeaderScript tabHeaderScript = newGO.GetComponent<TabHeaderScript>();
		if (tabHeaderScript != null)
		{
			int currentIndex = index;

			if (copiesOfContent > 1)
			{
				int originalContentSize = itemCount / copiesOfContent;
				if (index >= originalContentSize)
				{
					currentIndex = index % originalContentSize;
				}
			}

			tabHeaderScript.SetIndex(currentIndex, parentTabScript);
		}
	}
	
	private void CheckNestedScrollRectScript(GameObject newGO, int index)
	{
		NestedScrollRect nestedScrollRect = newGO.GetComponent<NestedScrollRect>();
		if (nestedScrollRect != null)
		{
			nestedScrollRect.SetParentScrollRectSnap(this);
		}
	}
	
	private void CheckCanvasGroupScript(GameObject newGO, int index)
	{
		CanvasGroupInScrollSnap canvasGroupScript = newGO.GetComponent<CanvasGroupInScrollSnap>();
		if (canvasGroupScript != null)
		{
			canvasGroupScript.Init(index, this);
		}
	}
	
	private void InitRepositionDistance()
	{
		if (itemCount == 1)
		{
			repositionAt = itemDistance * 10f;
		} else if (itemCount == 2)
		{
			repositionAt = 1.03f * itemDistance;
		} else if (itemCount % 2 == 0)
		{
			repositionAt = (itemCount / 2f + 0.5f) * itemDistance;
		} else
		{
			repositionAt = (itemCount / 2f) * itemDistance;
		}
			
	}

	IEnumerator InitItemDistance()
	{
		yield return new WaitForEndOfFrame();

		if (predefinedItemSize <= 0)
		{
			predefinedItemSize = (int) items[0].rect.width;
			itemDistance = items[0].rect.width + spacing;
			SetContentSize();
			InitRepositionDistance();
		}

		for (int i = 0; i < items.Length; i++)
		{
			items[i].anchoredPosition = new Vector2(itemDistance * i, items[i].anchoredPosition.y);
		}

		initDone = true;
	}

	private void FindClosestButtonIndex()
	{
		float minDistance = distance[0];
		closestItemIndex = 0;

		for (int i = 1; i < itemCount; i++)
		{
			if (distance[i] < minDistance)
			{
				closestItemIndex = i;
				minDistance = distance[i];
			}
		}
	}

	private void FindClosestSecondButtonIndex()
	{
		if (itemCount == 0)
		{
			return;
		}
			
		int rightItemIndex = closestItemIndex;
		int leftItemIndex = closestItemIndex;

		rightItemIndex++;
		if (rightItemIndex >= itemCount)
		{
			rightItemIndex = 0;
		}

		leftItemIndex--;
		if (leftItemIndex < 0)
		{
			leftItemIndex = itemCount - 1;
		}
			
		if (distance[rightItemIndex] > distance[leftItemIndex])
		{
			closestSecondItemIndex = leftItemIndex;
		}
		else
		{
			closestSecondItemIndex = rightItemIndex;
		}
	}

	private void CalculateDistances()
	{
		for (int i = 0; i < itemCount; i++)
		{
			distReposition[i] = center.position.x - center.transform.InverseTransformPoint(items[i].position).x;// - bttnDistance * 0.5f;
			distance[i] = Mathf.Abs(distReposition[i]);

			if (distReposition[i] > repositionAt)
			{
				float currentX = items[i].anchoredPosition.x;
				float currentY = items[i].anchoredPosition.y;

				Vector2 newAnchoredPosition = new Vector2(currentX + (itemCount * itemDistance), currentY);
				items[i].anchoredPosition = newAnchoredPosition;
			}

			if (distReposition[i] < -repositionAt)
			{
				float currentX = items[i].anchoredPosition.x;
				float currentY = items[i].anchoredPosition.y;

				Vector2 newAnchoredPosition = new Vector2(currentX - (itemCount * itemDistance), currentY);
				items[i].anchoredPosition = newAnchoredPosition;
			}

			if (scaling)
			{
				float clampedDistance = Mathf.Clamp(distance[i], 0f, itemDistance);
				float scaleModificator = 1f - clampedDistance / itemDistance;
				float newScale = minScale + (maxScale - minScale) * scaleModificator;
				items[i].transform.localScale = new Vector3(newScale, newScale, 1f);
			}
		}
	}

	private void LerpToItem(float position)
	{
		if (stopScrollAtNextElement)
		{
			if (closestItemIndex == startedDragAtItemIndex)
			{
				float velocity = scrollRect.velocity.x;
				float velocityAbsolete = Mathf.Abs(velocity);

				if (velocityAbsolete < itemDistance * snapToPreviousWhenPassed)
				{
					if (distance[closestItemIndex] > 0)
					{
						MoveToItem(closestItemIndex);
					}
				} else
				{
					if (velocity > 0)
					{
						MoveToItem(LeftElement());
					} else
					{
						MoveToItem(RightElement());
					}
				}
			} else
			{
				scrollRect.StopMovement();
				DoLerp(position);
			}
		} else
		{
			DoLerp(position);
		}
		
	}

	public void MoveToItem(int index)
	{
		scrollRect.StopMovement();
		moveToItem = index;
	}

	public void SetPosition(float newX)
	{
		Vector2 newPos = new Vector2(newX, content.anchoredPosition.y);
		content.anchoredPosition = newPos;
	}

	public void SetCurrentItem(int itemIndex)
	{
		if (itemIndex < 0 || itemIndex >= itemCount)
		{
			return;
		}

		Vector2 newPos = new Vector2(-items[itemIndex].anchoredPosition.x, content.anchoredPosition.y);
		content.anchoredPosition = newPos;
	}

	private void DoLerp(float position)
	{
		float newX = Mathf.Lerp(content.anchoredPosition.x, position, Time.deltaTime * snapSpeed);

		if (Mathf.Abs(newX - position) <= itemDistance * 0.005f)//Mathf.Epsilon)
		{
			newX = position;
			clickedSidePanel = false;

			if (stopScrollAtNextElement)
			{
				startedDragAtItemIndex = -1;
			}
		}

		if (moveToItem == closestItemIndex)
		{
			moveToItem = -1;
		}

		Vector2 newPos = new Vector2(newX, content.anchoredPosition.y);
		content.anchoredPosition = newPos;
	}

	public void BeginDrag()
	{
		dragging = true;
		clickedSidePanel = false;
		startedDragAtItemIndex = closestItemIndex;
	}

	public void EndDrag()
	{
		dragging = false;
	}

	public void ClickRight()
	{
		Debug.Log("click right");
		clickedSidePanel = true;
		closestItemIndex++;

		if (closestItemIndex >= itemCount)
		{
			closestItemIndex = 0;
		}

		closestSecondItemIndex++;

		if (closestSecondItemIndex >= itemCount)
		{
			closestSecondItemIndex = 0;
		}
	}

	public void ClickLeft()
	{
		Debug.Log("click left");
		clickedSidePanel = true;
		closestItemIndex--;

		if (closestItemIndex < 0)
		{
			closestItemIndex = itemCount - 1;
		}

		closestSecondItemIndex--;

		if (closestSecondItemIndex < 0)
		{
			closestSecondItemIndex = itemCount - 1;
		}
	}

	public ScrollRect GetScrollRect()
	{
		return scrollRect;
	}

	public bool IsInitDone()
	{
		return initDone;
	}

	private int RightElement()
	{
		int rightElement = closestItemIndex + 1;

		if (rightElement >= itemCount)
		{
			rightElement = 0;
		}

		return rightElement;
	}

	private int LeftElement()
	{
		int leftElement = closestItemIndex - 1;

		if (leftElement < 0)
		{
			leftElement = itemCount - 1;
		}

		return leftElement;
	}
		
	public void Clear()
	{
		copiesOfContent = 1;

		initDone = false;

		itemCount = 0;
		distance = null;
		distReposition = null;
		items = null;

		ClearContainer();
	}

	private void ClearContainer()
	{
		int childCount = content.childCount;
		for (int i = 0; i < childCount; i++)
		{
			Destroy (content.GetChild (i).gameObject);
		}
	}

	public void DuplicateContent(int numberOfCopies = 2)
	{
		if (numberOfCopies > 1)
		{
			copiesOfContent = numberOfCopies;

			int childCount = content.childCount;

			if (childCount > 0)
			{
				List<GameObject> originalContent = new List<GameObject>(childCount);

				for (int i = 0; i < childCount; i++)
				{
					originalContent.Add(content.GetChild (i).gameObject);
				}

				for (int copies = 1; copies < numberOfCopies; copies++)
				{
					for (int i = 0; i < childCount; i++)
					{
						GameObject go = Instantiate(originalContent[i], content) as GameObject;
					}
				}
			}
		} else
		{
			copiesOfContent = 1;
		}
	}

	public RectTransform GetItem(int index)
	{
		return items[index];
	}

	public RectTransform GetCurrentItem()
	{
		return items[closestItemIndex];
	}

	public int GetRealItemCount()
	{
		return itemCount / copiesOfContent;
	}
}
