using System;
using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Collections.Generic;

public class LobbyAwesomeScrollCardScript : MonoBehaviour
{
	public LOBBY_CARD_TYPE type;

	[SerializeField]
	private Image lockIcon = null;

	[SerializeField]
	private Animator animator = null;

	[SerializeField]
	private Image cardImage = null;

	[SerializeField]
	private Text additionalText = null;

	private HorizontalScrollSnap parentScroll;
	private int indexInScroll;
	public int id { get; set; }

	private bool isAvailable = true;

	public LobbyCardView lockedCardView { get; private set; }

	void Awake ()
	{
		SetOnClickListener();
	}

	public void Init(LOBBY_CARD_TYPE newType, List<Sprite> cardSprites)
	{
		id = -1;

		if (newType != type)
		{
			type = newType;
			cardImage.sprite = cardSprites[(int) newType];
		}
	}

	public void SetType(LOBBY_CARD_TYPE newType)
	{
		type = newType;
	}

	public Text GetAdditionalText()
	{
		return additionalText;
	}

	public bool IsAvailable()
	{
		return isAvailable;
	}

	public void SetAvailability(LobbyCardView cardView)
	{
		if (cardView == null || cardView.available)
		{
			if (!isAvailable)
			{
				animator.SetTrigger("Unlock");
				lockIcon.gameObject.SetActive(false);
				LobbyAwesomeScrollScript.instance.GetBottomPanelScript().SetBottomPanelUnlocked();
			} else
			{
				lockIcon.gameObject.SetActive(false);
				GetComponentInChildren<GrayScaleScript>().RemoveMaterialFromGameObjects();
			}

			isAvailable = true;
			lockedCardView = null;
		}
		else
		{
			GrayScaleScript grayScaleScript = GetComponentInChildren<GrayScaleScript>();
			grayScaleScript.AddMaterialToAllGameObjects();
			grayScaleScript.SetGrayScaleAmount(1f);
			lockIcon.gameObject.SetActive(true);
			isAvailable = false;
			lockedCardView = cardView;
		}
	}

	public void ForceSetAvailable()
	{
		lockIcon.gameObject.SetActive(false);
		GetComponentInChildren<GrayScaleScript>().RemoveMaterialFromGameObjects();
		isAvailable = true;
		lockedCardView = null;
	}

	public bool IsUnlockingAnimFinished()
	{
		if( animator.GetCurrentAnimatorStateInfo(0).IsName("Default"))
		{
			return true;
		} else
		{
			return false;
		}
	}

	public void AnimateTopAndBottomPanels()
	{
		StartCoroutine(AnimateTopAndBottomPanelsCoroutine());
	}

	IEnumerator AnimateTopAndBottomPanelsCoroutine()
	{
		float startedAt = Time.realtimeSinceStartup;
		float now = startedAt;
		float passedTime = 0;
		float animationTime = 0.5f;
		float colorMixTime = 0.13f;
		float colorStableTime = animationTime - colorMixTime * 2f;

		RectTransform cardTransform = gameObject.transform.GetChild(0).GetComponent<RectTransform>();
		RectTransform topTransform = LobbyAwesomeScrollScript.instance.GetHeaderPanelScript().GetTextRectTransform();
		RectTransform bottomTransform = LobbyAwesomeScrollScript.instance.GetBottomPanelScript().GetLockedPanel().GetComponent<RectTransform>();

		Vector2 topInitialPosition = topTransform.anchoredPosition;
		Vector2 bottomInitialPosition = bottomTransform.anchoredPosition;

		LobbyLockedBottomPanelScript lockedScript = LobbyAwesomeScrollScript.instance.GetBottomPanelScript().GetLockedPanelScript();

		while(now < startedAt + animationTime)
		{
			now = Time.realtimeSinceStartup;
			passedTime = now - startedAt;

			if (passedTime < colorMixTime)
			{
				lockedScript.SetTextColor(Color.Lerp(Color.white, Color.red, passedTime / colorMixTime));
			}
			else if (passedTime > colorMixTime + colorStableTime)
			{
				lockedScript.SetTextColor(Color.Lerp(Color.red, Color.white, (passedTime - colorMixTime - colorStableTime) / colorMixTime));
			} else
			{
				lockedScript.SetTextColor(Color.red);
			}

			topTransform.anchoredPosition = cardTransform.anchoredPosition;
			bottomTransform.anchoredPosition = cardTransform.anchoredPosition;

			yield return new WaitForEndOfFrame();
		}

		topTransform.anchoredPosition = topInitialPosition;
		bottomTransform.anchoredPosition = bottomInitialPosition;
		lockedScript.SetTextColor(Color.white);
	}

	public void OnClick()
	{
		if (lockedCardView != null)
		{
			animator.SetTrigger("LockedClick");
			AnimateTopAndBottomPanels();
		}
		else
		{
			switch (type)
			{
			case LOBBY_CARD_TYPE.STANDART:
			case LOBBY_CARD_TYPE.CONVERSION:
			case LOBBY_CARD_TYPE.PAIR:
			case LOBBY_CARD_TYPE.TRUMPS:
			case LOBBY_CARD_TYPE.WITH_OUT_TRUMPS:
				LobbyAwesomeScrollScript.instance.HideLobby();
				JoinGameCardClicked(type);
				break;

			case LOBBY_CARD_TYPE.CREATE:
				CreateGameCardClicked();
				break;

			case LOBBY_CARD_TYPE.TOURNAMENT:
				TournamentCardClicked();
				break;

			case LOBBY_CARD_TYPE.SINGLE_TOURNAMENT:
				SingleTournamentCardClicked();
				break;
			}
		}
	}

	private void JoinGameCardClicked(LOBBY_CARD_TYPE gameType)
	{
		if (DataScript.instance == null) return;

		List<int> gameTypesList = new List<int>(1);
		gameTypesList.Add((int) gameType);

		GameFiltersData data = DataScript.instance.GetGameFilters (false);

		DurakCommandHelper.GetGamesByFilter(gameTypesList, data.betSizes[0], data.betSizes[1]);
	}

	private void CreateGameCardClicked()
	{
		if (LobbyScript.instance == null) return;

		LobbyScript.instance.OnCreateGameButtonPressed ();
	}

	private void TournamentCardClicked()
	{
		if (LobbyAwesomeScrollScript.instance == null) return;

		LobbyAwesomeScrollScript.instance.OnTournamentCardClicked ();
	}

	private void SingleTournamentCardClicked()
	{
		if (LobbyAwesomeScrollScript.instance == null) return;

		LobbyAwesomeScrollScript.instance.OnSingleTournamentCardClicked (indexInScroll);
	}

	public void SetParentScroll(HorizontalScrollSnap parent, int itemIndex)
	{
		parentScroll = parent;
		indexInScroll = itemIndex;
	}

	public bool IsAlmostCentered()
	{
		return IsCentered(0.1f);
	}

	public bool IsCentered()
	{
		return IsCentered(0.01f);
	}

	public bool IsCentered(float multiplier)
	{
		if (parentScroll.distance[indexInScroll] < parentScroll.itemDistance * multiplier)
		{
			return true;
		} else
		{
			return false;
		}
	}

	private void MoveToCenter()
	{
		parentScroll.MoveToItem(indexInScroll);
	}
		
	private void SetOnClickListener()
	{
		Button button = gameObject.GetComponentInChildren<Button>();
		button.onClick.RemoveAllListeners ();
		button.onClick.AddListener(delegate
			{
				if (LobbyAwesomeMatchMaker.instance.isInQueue || !LobbyAwesomeScrollScript.instance.CanClick ())
				{
					return;
				}

				if (IsAlmostCentered ())
				{
					OnClick ();
				} else
				{
					MoveToCenter ();
				}
			}
		);
	}
}